package com.intumit.solr.dataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.dataset.QueryElevationComponent.ElevationObj;
import com.intumit.viewRecord.ViewRecordFacade;

import flexjson.JSONDeserializer;

public abstract class ElevatorSetFacade {
	static ElevatorSetFacade instance = null;

	/**
	 * ?��? Singleton
	 * 
	 * @return
	 */
	public static ElevatorSetFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkElevatorSetFacade();
			}
			else {
				instance = new DatabaseElevatorSetFacade();
			}
		}
		return instance;
	}

	private ElevatorSetFacade() {
	}

	public abstract void save(String elevatorSetName, String dsId);
	public abstract void saveOrUpdate(ElevatorSet elevatorSet);
	public abstract void delete(String id, String dsId) throws Exception;
	public abstract List<ElevatorSet> listAll();
	public abstract List<ElevatorSet> listAll(String dsId);
	public abstract ElevatorSet get(String dsIdStr, String query);
	public abstract ElevationObj loadElevationMap(QueryElevationComponent qec, String dsIdStr, String qstr, boolean dontHide);
	public abstract ElevatorSet get(Long id);

	public static ElevationObj toElevationMap(QueryElevationComponent qec, ElevatorSet eSet, String qstr, boolean dontHide) {
		List<String> allHidden = new ArrayList<String>();
		List<String> fixedAtTopAsList = new ArrayList<String>();
		
		if (eSet != null) {
			fixedAtTopAsList.addAll(eSet.getFixedAtTopAsList());
			
			if (!dontHide) {
				allHidden.addAll(eSet.getHiddenList());
			}
		}
		
		ElevationObj obj = null;
		
		try {
			List<String> topViewList = ViewRecordFacade.getTopViewRecordId(qstr, 10);
			topViewList.removeAll(fixedAtTopAsList); // Make sure elements already in fixedAtTopAsList will not be reordered.
			fixedAtTopAsList.addAll(topViewList);
			obj = qec.new ElevationObj( qstr, fixedAtTopAsList, dontHide ? null : allHidden );
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		finally {
			try {
				 obj = qec.new ElevationObj( qstr, fixedAtTopAsList, dontHide ? null : allHidden );
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		return obj;
	}
	

	public static class ZkElevatorSetFacade extends ElevatorSetFacade {
		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/elevatorset";
		
		boolean cacheDirty = true;
		Table<String, String, ElevatorSet> cache = HashBasedTable.create();
		PathChildrenCache pathCache = null;

		public ZkElevatorSetFacade() {
			super();
			CuratorFramework client = SearchManager.getCuratorClient();
			
			try {
				Stat stat = client.checkExists().forPath(MYPATH);
				if (stat == null) {
					client.create().creatingParentsIfNeeded().forPath(MYPATH + "/" + ID_SEED);
					DistributedAtomicInteger seed = new DistributedAtomicInteger(client, MYPATH + "/" + ID_SEED, new ExponentialBackoffRetry(1000, 3));
					seed.forceSet(0);
				}
				
				pathCache = new PathChildrenCache(client, MYPATH, true);
				pathCache.start();
				
				addListener(pathCache);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	    private void addListener(PathChildrenCache pCache)
	    {
	        // a PathChildrenCacheListener is optional. Here, it's used just to log changes
	        PathChildrenCacheListener listener = new PathChildrenCacheListener()
	        {
	            @Override
	            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception
	            {
	                switch ( event.getType() )
	                {
	                    case CHILD_ADDED:
	                    case CHILD_UPDATED:
	                    case CHILD_REMOVED:
	                    {
	                        System.out.println("ElevatorSet Node dirty, going to reload cache: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
	                        cacheDirty = true;
	                        break;
	                    }
	                }
	            }
	        };
	        pCache.getListenable().addListener(listener);
	    }
		
		private int generateNewDsId() {

			try {
				CuratorFramework client = SearchManager.getCuratorClient();
				DistributedAtomicInteger seed = new DistributedAtomicInteger(client, MYPATH + "/" + ID_SEED, new ExponentialBackoffRetry(1000, 3));
				
				AtomicValue<Integer> currentValue = seed.increment();
				
				if (currentValue.succeeded()) {
					return currentValue.postValue();
				}
				else
					throw new RuntimeException("Cannot auto increase the AtomicValue in ZooKeeper:" + MYPATH + "/" + ID_SEED);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			throw new RuntimeException("Cannot auto increase the AtomicValue in ZooKeeper:" + MYPATH + "/" + ID_SEED);
		}
		

		@Override
		public void save(String elevatorSetName, String dsId) {
			try {
				Integer intSeed = generateNewDsId();
				
				String dsZkPath = MYPATH + "/" + intSeed;
				ElevatorSet elevatorSet = new ElevatorSet();
				elevatorSet.setDsId(dsId);
				elevatorSet.setId(intSeed.longValue());

				SearchManager.getCuratorClient().create().creatingParentsIfNeeded().forPath(dsZkPath, elevatorSet.serializeToJsonString().getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void delete(String id, String dsId) throws Exception {
			String dsZkPath = MYPATH + "/" + id;
			CuratorFramework client = SearchManager.getCuratorClient();
			Stat stat = client.checkExists().forPath(dsZkPath);
			
			if (stat != null) {
				client.delete().forPath(dsZkPath);
			}
		}

		@Override
		public List<ElevatorSet> listAll(String dsId) {
			if (cacheDirty) {
				reloadCache();
			}
			List<ElevatorSet> list = new ArrayList<ElevatorSet>();
			
			if (cache.containsColumn(dsId))
				list.addAll(cache.column(dsId).values());
			
			return list;
		}

		@Override
		public ElevatorSet get(String dsIdStr, String query) {
			if (cacheDirty) {
				reloadCache();
			}
			return cache.get(dsIdStr, query);
		}

		@Override
		public ElevationObj loadElevationMap(QueryElevationComponent qec,
				String dsIdStr, String qstr, boolean dontHide) {

			if (cacheDirty) {
				reloadCache();
			}
			ElevatorSet eSet = get(dsIdStr, qstr);
			List<String> allHidden = new ArrayList<String>();
			
			if (!dontHide) {
				List<ElevatorSet> list = cache.containsRow(qstr) ? new ArrayList<ElevatorSet>(cache.row(qstr).values()) : new ArrayList<ElevatorSet>();
				
				for (ElevatorSet es: list) {
					allHidden.addAll(es.getHiddenList());
				}
			}
			
			List<String> fixedAtTopAsList = new ArrayList<String>();
			
			if (eSet != null) {
				//try {
					fixedAtTopAsList.addAll(eSet.getFixedAtTopAsList());
					//ElevationObj obj = qec.new ElevationObj( qstr, eSet.getFixedAtTopAsList(), dontHide ? null : allHidden );
					//return obj;
				/*}
				catch (IOException e) {
					e.printStackTrace();
				}*/
			}
			
			try {
				List<String> topViewList = ViewRecordFacade.getTopViewRecordId(qstr, 10);
				topViewList.removeAll(fixedAtTopAsList); // Make sure elements already in fixedAtTopAsList will not be reordered.
				fixedAtTopAsList.addAll(topViewList);
				ElevationObj obj = qec.new ElevationObj( qstr, fixedAtTopAsList, dontHide ? null : allHidden );
				return obj;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			return null;
		}

		@Override
		public void saveOrUpdate(ElevatorSet eSet) {
			try {
				if (eSet.getId() == null || eSet.getId() == 0) {
					eSet.setId((long)generateNewDsId());
				}
				String dsZkPath = MYPATH + "/" + eSet.getId();
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);
				
				if (stat == null) {
					client.create().forPath(dsZkPath, eSet.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, eSet.serializeToJsonString().getBytes());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public ElevatorSet get(Long id) {
			try {
				String dsZkPath = MYPATH + "/" + id;
				String jsonStr = new String(pathCache.getCurrentData(dsZkPath).getData());
				ElevatorSet ds = new JSONDeserializer<ElevatorSet>().deserialize( jsonStr );
				
				return ds;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}

		@Override
		public List<ElevatorSet> listAll() {
			if (cacheDirty) {
				reloadCache();
			}
			List<ElevatorSet> list = new ArrayList<ElevatorSet>();
			list.addAll(cache.values());
			return list;
		}
		
		
		private synchronized void reloadCache() {
			if (cacheDirty) 
				cache.clear();
			
			try {
				for (ChildData child: pathCache.getCurrentData()) {
					if ( (MYPATH + "/" + ID_SEED).equals(child.getPath()) )
						continue;
					
					String jsonStr = new String(child.getData());
					
					if (StringUtils.isNotEmpty(jsonStr)) {
						ElevatorSet es = new JSONDeserializer<ElevatorSet>().deserialize( jsonStr );
						cache.put(es.getDsId(),  es.getQuery(), es);
					}
				}
				
				cacheDirty = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	
	
	public static class DatabaseElevatorSetFacade extends ElevatorSetFacade {
	
		public synchronized void save(String elevatorSetName, String dsId) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ElevatorSet elevatorSet = new ElevatorSet();
				elevatorSet.setDsId(dsId);
				ses.saveOrUpdate(elevatorSet);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
		
		public synchronized void saveOrUpdate(ElevatorSet elevatorSet) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.saveOrUpdate(elevatorSet);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
		
		public synchronized void delete(String id, String dsId) throws Exception {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.createQuery("delete from " + ElevatorSet.class.getName() + " where id=" + id + " AND dsId='" + dsId + "'")
						.executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
		public synchronized List<ElevatorSet> listAll() {
			return list(null, null);
		}
		
		public synchronized List<ElevatorSet> listAll(String dsId) {
			return list(dsId, null);
		}
		public synchronized ElevatorSet get(String dsIdStr, String query) {
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(ElevatorSet.class).addOrder(Order.desc("timestamp")).addOrder(Order.desc("id"));
				if (dsIdStr != null)
					ct.add(Restrictions.eq("dsId", dsIdStr));
				if (query != null)
					ct.add(Restrictions.eq("query", query));
				
				List result = ct.list();
				
				if (result != null && result.size() > 0) {
					return (ElevatorSet)result.get(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}
	
			return null;
		}
		
		public synchronized ElevatorSet get(Long id) {
			try {
				return (ElevatorSet)HibernateUtil.getSession().get(ElevatorSet.class, id);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
	
			return null;
		}
		
		@SuppressWarnings("unchecked")
		private List list(String dsId, String query) {
			List result = new ArrayList();
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(ElevatorSet.class).addOrder(Order.desc("timestamp")).addOrder(Order.desc("id"));
				if (dsId != null)
					ct.add(Restrictions.eq("dsId", dsId));
				if (query != null)
					ct.add(Restrictions.eq("query", query));
				result = ct.list();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}
	
			return result;
		}
		

		public ElevationObj loadElevationMap(QueryElevationComponent qec, String dsIdStr, String qstr, boolean dontHide) {
			
			//List<ElevatorSet> eList = listAll(mp);
			ElevatorSet eSet = get(dsIdStr, qstr);
			List<String> allHidden = new ArrayList<String>();
			
			if (!dontHide) {
				List<ElevatorSet> list = list(null, qstr);
				
				for (ElevatorSet es: list) {
					allHidden.addAll(es.getHiddenList());
				}
			}
			
			List<String> fixedAtTopAsList = new ArrayList<String>();
			
			if (eSet != null) {
				//try {
					fixedAtTopAsList.addAll(eSet.getFixedAtTopAsList());
					//ElevationObj obj = qec.new ElevationObj( qstr, eSet.getFixedAtTopAsList(), dontHide ? null : allHidden );
					//return obj;
				/*}
				catch (IOException e) {
					e.printStackTrace();
				}*/
			}
			
			try {
				List<String> topViewList = ViewRecordFacade.getTopViewRecordId(qstr, 10);
				topViewList.removeAll(fixedAtTopAsList); // Make sure elements already in fixedAtTopAsList will not be reordered.
				fixedAtTopAsList.addAll(topViewList);
				ElevationObj obj = qec.new ElevationObj( qstr, fixedAtTopAsList, dontHide ? null : allHidden );
				return obj;
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			return null;
		}
		
	}
}
