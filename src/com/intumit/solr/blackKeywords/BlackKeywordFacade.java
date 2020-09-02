package com.intumit.solr.blackKeywords;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;

import flexjson.JSONDeserializer;

public abstract class BlackKeywordFacade {
	public static final String DEFAULT_FUNC = "default";
	static BlackKeywordFacade instance = null;
	public static final int MAX_KEYWORD_LENGTH = 128;

	private static long lastLoadTimestamp = -1;
	private static long CHECK_PERIOD = 5 * 60 * 1000;
	private static HashSet<String> blackList = null;
    
	/**
	 * 改成 Singleton
	 *
	 * @return
	 */
	public static BlackKeywordFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkBlackKeywordFacade();
			}
			else {
				instance = new DatabaseBlackKeywordFacade();
			}
		}
		return instance;
	}
	
	public abstract HashSet<String> loadBlackList();
	public abstract List<BlackKeyword> listBlackKeywords();
	public abstract List<BlackKeyword> listBlackKeywords(String searchKeyword);
	
	public abstract void save(String keyword);
	public abstract void update(String id, String keyword) throws Exception;
	public abstract void delete(long id) throws Exception;
	public abstract boolean isBlack(String searchKeyword);
	

	public static class ZkBlackKeywordFacade extends BlackKeywordFacade {
		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/blackkeyword";
		
		boolean cacheDirty = true;
		PathChildrenCache cache = null;
		HashSet<String> keywordList = null;

		public ZkBlackKeywordFacade() {
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
	                        System.out.println("BlackKeyword Node dirty, going to reload cache: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
	                        cacheDirty = true;
	                        break;
	                    }
	                }
	            }
	        };
	        pCache.getListenable().addListener(listener);
	    }
		
		private long generateNewDsId() {

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
		public void save(String keyword) {
			try {
				Long intSeed = generateNewDsId();
				
				BlackKeyword blackKeyword = new BlackKeyword();
				blackKeyword.setId(intSeed);
				blackKeyword.setKeyword(keyword);
				
				String dsZkPath = MYPATH + "/" + intSeed;

				SearchManager.getCuratorClient().create().forPath(dsZkPath, blackKeyword.serializeToJsonString().getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void update(String id, String keyword) {
			try {
				BlackKeyword blackKeyword = new BlackKeyword();
				blackKeyword.setId(Long.parseLong(id));
				blackKeyword.setKeyword(keyword);
				blackKeyword.setSaveTime(new Timestamp(System.currentTimeMillis()));
				
				String dsZkPath = MYPATH + "/" + id;
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);
				
				if (stat == null) {
					client.create().forPath(dsZkPath, blackKeyword.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, blackKeyword.serializeToJsonString().getBytes());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void delete(long id) throws Exception {
			String dsZkPath = MYPATH + "/" + id;
			CuratorFramework client = SearchManager.getCuratorClient();
			Stat stat = client.checkExists().forPath(dsZkPath);
			
			if (stat != null) {
				client.delete().forPath(dsZkPath);
			}
		}

		@Override
		public List<BlackKeyword> listBlackKeywords() {
			
			List<BlackKeyword> list = new ArrayList<BlackKeyword>();
			try {
				if (cacheDirty) {
					cache.rebuild();
					reload();
					cacheDirty = false;
				}
				
				List<ChildData> children = cache.getCurrentData();
				
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						BlackKeyword ds = new JSONDeserializer<BlackKeyword>().deserialize( jsonStr );
						list.add(ds);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		@Override
		public List<BlackKeyword> listBlackKeywords(String searchKeyword) {
			
			List<BlackKeyword> list = new ArrayList<BlackKeyword>();
			try {
				if (cacheDirty) {
					cache.rebuild();
					reload();
					cacheDirty = false;
				}
				
				List<ChildData> children = cache.getCurrentData();
				
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						BlackKeyword ds = new JSONDeserializer<BlackKeyword>().deserialize( jsonStr );
						
						if (StringUtils.contains(ds.getKeyword(), searchKeyword))
							list.add(ds);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}
		

		@Override
		public boolean isBlack(String searchKeyword) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					reload();
					cacheDirty = false;
				}
				if (keywordList.contains(searchKeyword))
					return true;
				
				for (String k: keywordList) {
					if (StringUtils.contains(k, searchKeyword))
						return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public HashSet<String> loadBlackList() {
			try {
				if (cacheDirty) {
					cache.rebuild();
					reload();
					cacheDirty = false;
				}
				
				List<ChildData> children = cache.getCurrentData();
				
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						BlackKeyword ds = new JSONDeserializer<BlackKeyword>().deserialize( jsonStr );
						keywordList.add(ds.getKeyword());
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return keywordList;
		}

		public BlackKeyword get(int id) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					reload();
					cacheDirty = false;
				}
				String dsZkPath = MYPATH + "/" + id;
				String jsonStr = new String(cache.getCurrentData(dsZkPath).getData());
				
				BlackKeyword ds = new JSONDeserializer<BlackKeyword>().deserialize( jsonStr );
				
				return ds;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}

		synchronized void reload() {
			try {
				if (keywordList == null)
					keywordList = new HashSet<String>();
				else
					keywordList.clear();
				
				List<ChildData> children = cache.getCurrentData();
				
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						BlackKeyword ds = new JSONDeserializer<BlackKeyword>().deserialize( jsonStr );
						keywordList.add(ds.getKeyword());
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class DatabaseBlackKeywordFacade extends BlackKeywordFacade {
		@Override
	    public HashSet<String> loadBlackList() {
			if (lastLoadTimestamp + CHECK_PERIOD < System.currentTimeMillis() || blackList == null) {
				List<BlackKeyword> list = BlackKeywordFacade.getInstance().listBlackKeywords();
				
				blackList = new HashSet<String>();
				for (BlackKeyword kw: list) {
					blackList.add(kw.getKeyword());
				}
				lastLoadTimestamp = System.currentTimeMillis();
			}
			return blackList;
		}
	    
	    @Override
		public synchronized void save(String keyword) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				BlackKeyword word = new BlackKeyword();
				word.setKeyword(keyword);
				word.setSaveTime(new Timestamp(System.currentTimeMillis()));
				ses.saveOrUpdate(word);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
				blackList = null;
			}
		}
	
		@Override
		public synchronized void update(String id, String keyword) throws Exception {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				BlackKeyword word = new BlackKeyword();
				word.setId(Long.parseLong(id));
				word.setKeyword(keyword);
				word.setSaveTime(new Timestamp(System.currentTimeMillis()));
				ses.update(word);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
				blackList = null;
			}
		}
	
		@Override
		public synchronized void delete(long id) throws Exception {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.createQuery("delete from " + BlackKeyword.class.getName() + " Where id=" + id)
						.executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
				blackList = null;
			}
		}
	
	
		@Override
		public synchronized List<BlackKeyword> listBlackKeywords() {
			return list();
		}
	
		@Override
		public synchronized List<BlackKeyword> listBlackKeywords(String searchKeyword) {
			return list(searchKeyword);
		}
	
		@SuppressWarnings("unchecked")
		private List list() {
			List result = new ArrayList();
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(BlackKeyword.class);
				Order order = Order.desc("saveTime");
				ct.addOrder(order);
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
	
		private List list(String searchKeyword) {
			List result = new ArrayList();
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(BlackKeyword.class);
				Criterion c1 = (Restrictions.like("keyword","%" + searchKeyword + "%"));
				ct.add(c1);
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
	
	
		@Override
		public boolean isBlack(String searchKeyword) {
			List result = new ArrayList();
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(BlackKeyword.class);
				Criterion c1 = (Restrictions.like("keyword","%" + searchKeyword + "%"));
				ct.add(c1);
				result = ct.list();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
	
			if(result.size()!=0)
			    return true;
			else
			    return false;
		}
	
	
		public List<String> filterBlackWord(List<String> word) {
		    for(int i=0 ; i<word.size() ; i++){
	            if(isBlack(word.get(i)))
	         	   word.remove(i);
			}
		    return word;
		}

	}









}
