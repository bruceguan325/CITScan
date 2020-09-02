package com.intumit.solr.dataset;

import java.util.ArrayList;
import java.util.List;

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

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.admin.GroupDataSet;

import flexjson.JSONDeserializer;

public abstract class DataSetFacade {
	static DataSetFacade instance = null;

	/**
	 * Singleton
	 *
	 * @return
	 */
	public static DataSetFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkDataSetFacade();
			}
			else {
				instance = new DatabaseDataSetFacade();
			}
		}
		return instance;
	}

	private DataSetFacade() {
	}

	public abstract void save(String dataSetName);
	public abstract void saveOrUpdate(DataSet dataSet);
	public abstract void delete(int id) throws Exception;
	public abstract List<DataSet> listAll();
	public abstract List<DataSet> listAllEnabled();
	public abstract DataSet get(int id);
	
	
	public static class ZkDataSetFacade extends DataSetFacade {
		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/dataset";
		
		boolean cacheDirty = true;
		PathChildrenCache cache = null;

		public ZkDataSetFacade() {
			super();
			CuratorFramework client = SearchManager.getCuratorClient();
			
			try {
				Stat stat = client.checkExists().forPath(MYPATH);
				if (stat == null) {
					client.create().creatingParentsIfNeeded().forPath(MYPATH + "/" + ID_SEED);
					DistributedAtomicInteger seed = new DistributedAtomicInteger(client, MYPATH + "/" + ID_SEED, new ExponentialBackoffRetry(1000, 3));
					seed.forceSet(0);
				}
				
				cache = new PathChildrenCache(client, MYPATH, true);
				cache.start();
				
				addListener(cache);
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
	                        System.out.println("DataSet Node dirty, going to reload cache: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
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
		public void save(String dataSetName) {
			try {
				Integer intSeed = generateNewDsId();
				
				DataSet dataSet = new DataSet();
				dataSet.setId(intSeed);
				dataSet.setName(dataSetName);
				dataSet.setCoreName("core0");
				dataSet.setFieldMltCount(4L);
				dataSet.setFieldWikiCount(10L);
				
				String dsZkPath = MYPATH + "/" + intSeed;

				SearchManager.getCuratorClient().create().forPath(dsZkPath, dataSet.serializeToJsonString().getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void saveOrUpdate(DataSet dataSet) {
			try {
				if (dataSet.getId() == 0) {
					dataSet.setId(generateNewDsId());
				}
				String dsZkPath = MYPATH + "/" + dataSet.getId();
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);
				
				if (stat == null) {
					client.create().forPath(dsZkPath, dataSet.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, dataSet.serializeToJsonString().getBytes());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void delete(int id) throws Exception {
			String dsZkPath = MYPATH + "/" + id;
			CuratorFramework client = SearchManager.getCuratorClient();
			Stat stat = client.checkExists().forPath(dsZkPath);
			
			if (stat != null) {
				client.delete().forPath(dsZkPath);
			}
		}

		@Override
		public List<DataSet> listAll() {
			
			
			List<DataSet> list = new ArrayList<DataSet>();
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				
				List<ChildData> children = cache.getCurrentData();
				
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						DataSet ds = new JSONDeserializer<DataSet>().deserialize( jsonStr );
						list.add(ds);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		@Override
		public List<DataSet> listAllEnabled() {
			List<DataSet> list = new ArrayList<DataSet>();
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				List<ChildData> children = cache.getCurrentData();
				
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						DataSet ds = new JSONDeserializer<DataSet>().deserialize( jsonStr );
						if (ds.getEnable()) list.add(ds);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		@Override
		public DataSet get(int id) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				String dsZkPath = MYPATH + "/" + id;
				String jsonStr = new String(cache.getCurrentData(dsZkPath).getData());
				
				DataSet ds = new JSONDeserializer<DataSet>().deserialize( jsonStr );
				
				return ds;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
	}
	
	public static class DatabaseDataSetFacade extends DataSetFacade {
		
		public synchronized void save(String dataSetName) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				DataSet dataSet = new DataSet();
				dataSet.setName(dataSetName);
				dataSet.setCoreName("core0");
				dataSet.setFieldMltCount(4L);
				dataSet.setFieldWikiCount(10L);
				ses.saveOrUpdate(dataSet);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
	
		public synchronized void saveOrUpdate(DataSet dataSet) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.saveOrUpdate(dataSet);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
	
		public synchronized void delete(int id) throws Exception {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.createQuery("delete from " + GroupDataSet.class.getName() + " where dataSetId=" + id)
		                .executeUpdate();
				ses.createQuery("delete from " + DataSet.class.getName() + " where id=" + id)
						.executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
	
		public synchronized List<DataSet> listAll() {
			return list();
		}
		public synchronized List<DataSet> listAllEnabled() {
			List result = new ArrayList();
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(DataSet.class)
							.addOrder(Order.asc("dsOrder"))
							.addOrder(Order.asc("id"));
				ct.add( Restrictions.ne("enable", Boolean.FALSE ) );
					
				result = ct.list();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
	
			return result;
		}
		public synchronized DataSet get(int id) {
			try {
				return (DataSet)HibernateUtil.getSession().get(DataSet.class, id);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
	
			return null;
		}
	
		@SuppressWarnings("unchecked")
		private List list() {
			List result = new ArrayList();
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(DataSet.class).addOrder(Order.asc("dsOrder")).addOrder(Order.asc("id"));
				result = ct.list();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
	
			return result;
		}
	}
}
