package com.intumit.solr.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;

import flexjson.JSONDeserializer;

public abstract class AdminLocationFacade {
	public static final String SESSION_KEY = "_HHAUSK_";
	static AdminLocationFacade instance = null;
	
	static final AdminLocation EMPTY_ADMIN_location = new AdminLocation();

	/**
	 * Singleton
	 * 
	 * @return
	 */
	public static AdminLocationFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkAdminLocationFacade();
			}
			else {
				instance = new DatabaseAdminLocationFacade();
			}
		}
		return instance;
	}

	private AdminLocationFacade() {
	}
	
	public abstract AdminLocation find(String ip, String loginName);

	public abstract void save(String ipAddr);

	public abstract void saveOrUpdate(AdminLocation adminLoc);

	public abstract void delete(int id) throws Exception;

	public abstract List<AdminLocation> listAll();

	public abstract AdminLocation get(int id);
	
	public abstract Set<AdminLocation> findAll(String ip, String loginName);


	public static class ZkAdminLocationFacade extends AdminLocationFacade {

		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/admin/location";
		
		boolean cacheDirty = true;
		PathChildrenCache cache = null;

		public ZkAdminLocationFacade() {
			CuratorFramework client = SearchManager.getCuratorClient();
			
			try {
				Stat stat = client.checkExists().forPath(MYPATH);
				if (stat == null) {
					client.create().creatingParentsIfNeeded().forPath(MYPATH + "/" + ID_SEED);
					DistributedAtomicInteger seed = new DistributedAtomicInteger(client, MYPATH + "/" + ID_SEED, new ExponentialBackoffRetry(1000, 3));
					seed.forceSet(0);

					AdminLocation defaultRule = new AdminLocation();
					defaultRule.setLoginName("*");
					defaultRule.setNoNeedLogin(true);
					defaultRule.setIpAddress("127.0.0.1");
					defaultRule.setUrlRegex(".*");
					saveOrUpdate(defaultRule);
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
	                        System.out.println("AdminLocation Node dirty, going to reload cache: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
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
		public AdminLocation find(String ip, String loginName) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				
				List<ChildData> children = cache.getCurrentData();
				String seperator = ip.indexOf('.') != -1 ? "." : ":";
				String ipPart = ip + seperator;
				
				do {
					ipPart = StringUtils.substringBeforeLast(ipPart, seperator);
					
					for (ChildData child: children) {
						if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
							String jsonStr = new String(child.getData());
							AdminLocation ds = new JSONDeserializer<AdminLocation>().deserialize( jsonStr );
							
							if (ds.getIpAddress().equals(ipPart)) {
								if (Boolean.TRUE.equals(ds.getNoNeedLogin())) {
									return ds;
								}
								
								if ((loginName != null && ds.getLoginName().equals("*")) || ds.getLoginName().equals(loginName)) {
									return ds;
								}
							}
						}
					}
				}
				while (StringUtils.lastIndexOf(ipPart, seperator) != -1);

				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						AdminLocation ds = new JSONDeserializer<AdminLocation>().deserialize( jsonStr );

						if (ds.getIpAddress().equals("0.0.0.0")) {
							if (Boolean.TRUE.equals(ds.getNoNeedLogin())) {
								return ds;
							}
							
							if ((loginName != null && ds.getLoginName().equals("*")) || ds.getLoginName().equals(loginName)) {
								return ds;
							}
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void save(String ipAddr) {
			try {
				Integer intSeed = generateNewDsId();
				
				AdminLocation adminLoc = new AdminLocation();
				adminLoc.setId(intSeed);
				adminLoc.setIpAddress(ipAddr);
				
				String dsZkPath = MYPATH + "/" + intSeed;

				SearchManager.getCuratorClient().create().forPath(dsZkPath, adminLoc.serializeToJsonString().getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void saveOrUpdate(AdminLocation adminLoc) {
			try {
				if (adminLoc.getId() == 0) {
					adminLoc.setId(generateNewDsId());
				}
				String dsZkPath = MYPATH + "/" + adminLoc.getId();
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);
				
				if (stat == null) {
					client.create().forPath(dsZkPath, adminLoc.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, adminLoc.serializeToJsonString().getBytes());
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
		public List<AdminLocation> listAll() {
			
			
			List<AdminLocation> list = new ArrayList<AdminLocation>();
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				
				List<ChildData> children = cache.getCurrentData();
				
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						AdminLocation ds = new JSONDeserializer<AdminLocation>().deserialize( jsonStr );
						list.add(ds);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		@Override
		public AdminLocation get(int id) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				String dsZkPath = MYPATH + "/" + id;
				String jsonStr = new String(cache.getCurrentData(dsZkPath).getData());
				
				AdminLocation ds = new JSONDeserializer<AdminLocation>().deserialize( jsonStr );
				
				return ds;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return null;
		}

		@Override
		public Set<AdminLocation> findAll(String ip, String loginName) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				
				List<ChildData> children = cache.getCurrentData();
				String seperator = ip.indexOf('.') != -1 ? "." : ":";
				String ipPart = ip + seperator;
				
				do {
					ipPart = StringUtils.substringBeforeLast(ipPart, seperator);
					Set<AdminLocation> all = new HashSet<AdminLocation>();
					for (ChildData child: children) {
						if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
							String jsonStr = new String(child.getData());
							AdminLocation ds = new JSONDeserializer<AdminLocation>().deserialize( jsonStr );
							
							if (ds.getIpAddress().equals(ipPart)) {
								if (Boolean.TRUE.equals(ds.getNoNeedLogin())) {
									all.add(ds);
								}
								
								if ((loginName != null && ds.getLoginName().equals("*")) || ds.getLoginName().equals(loginName)) {
									all.add(ds);
								}
							}
						}
					}
					
					if(!all.isEmpty()) {
						return all;
					}
				}
				while (StringUtils.lastIndexOf(ipPart, seperator) != -1);
				Set<AdminLocation> all = new HashSet<AdminLocation>();
				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						AdminLocation ds = new JSONDeserializer<AdminLocation>().deserialize( jsonStr );

						if (ds.getIpAddress().equals("0.0.0.0")) {
							if (Boolean.TRUE.equals(ds.getNoNeedLogin())) {
								all.add(ds);
							}
							
							if ((loginName != null && ds.getLoginName().equals("*")) || ds.getLoginName().equals(loginName)) {
								all.add(ds);
							}
						}
					}
				}
				if(!all.isEmpty())
					return all;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		
	}

	public static class DatabaseAdminLocationFacade extends AdminLocationFacade {
		
		public DatabaseAdminLocationFacade() {
			if (listAll().size() == 0) {
				AdminLocation defaultRule = new AdminLocation();
				defaultRule.setLoginName("*");
				defaultRule.setNoNeedLogin(true);
				defaultRule.setIpAddress("127.0.0.1");
				defaultRule.setUrlRegex(".*");
				saveOrUpdate(defaultRule);
				
				AdminLocation defaultRule2 = new AdminLocation();
				defaultRule2.setLoginName("*");
				defaultRule2.setNoNeedLogin(false);
				defaultRule2.setIpAddress("0.0.0.0");
				defaultRule2.setUrlRegex(".*");
				saveOrUpdate(defaultRule2);
			}
		}

		public AdminLocation find(String ip, String loginName) {
			AdminLocation loc = null;
			String ipPart = ip + ".";
			
			do {
				ipPart = StringUtils.substringBeforeLast(ipPart, ".");
				List<AdminLocation> list = get(ipPart, loginName);
				
				if (list != null && list.size() > 0) {
					loc = list.get(0);
				}
			}
			while (StringUtils.lastIndexOf(ipPart, ".") != -1);

			if (loc == null) {
				List<AdminLocation> list = get("0.0.0.0", loginName);
				
				if (list != null && list.size() > 0) {
					loc = list.get(0);
				}
			}
			
			return loc;
		}
		
		public synchronized void save(String ipAddr) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				AdminLocation adminLoc = new AdminLocation();
				adminLoc.setIpAddress(ipAddr);
				ses.saveOrUpdate(adminLoc);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
		
		public synchronized void saveOrUpdate(AdminLocation adminLoc) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.saveOrUpdate(adminLoc);
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
				ses.createQuery("delete from " + AdminLocation.class.getName() + " where id=" + id)
						.executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}
		
		public synchronized List<AdminLocation> listAll() {
			return list();
		}
			
		public synchronized AdminLocation get(int id) {
			try {
				return (AdminLocation)HibernateUtil.getSession().get(AdminLocation.class, id);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
	
			return null;
		}

		@SuppressWarnings("unchecked")
		private List get(String ipaddress, String loginName) {
			List result = new ArrayList();

			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(AdminLocation.class).addOrder(Order.asc("id"));
				ct.add( Restrictions.ilike("ipAddress", ipaddress, MatchMode.END) );
				ct.add( 
						loginName == null 
							? Restrictions.eq("noNeedLogin", true)
							: Restrictions.or(
									Restrictions.eq("noNeedLogin", true), 
									Restrictions.or(
													Restrictions.eq("loginName", loginName), 
													Restrictions.eq("loginName", "*")
													)
									)
							);
				result = ct.list();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}

			return result;
		}
		
		@SuppressWarnings("unchecked")
		private List list() {
			List result = new ArrayList();
	
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(AdminLocation.class).addOrder(Order.asc("id"));
	//			if (xdmpId != null)
	//				ct.add( Restrictions.eq("xdmp", xdmpId ) );
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
		public Set<AdminLocation> findAll(String ip, String loginName) {
			String ipPart = ip + ".";
			Set<AdminLocation> all = new HashSet<AdminLocation>();
			do {
				ipPart = StringUtils.substringBeforeLast(ipPart, ".");
				List<AdminLocation> list = get(ipPart, loginName);
				
				if (list != null && list.size() > 0) {
					for(AdminLocation al : list) {
						if(StringUtils.equals(al.getIpAddress(), ipPart)) {
							all.add(al);
						}
					}
				}
			}
			while (StringUtils.lastIndexOf(ipPart, ".") != -1);

			if(!all.isEmpty()) {
				return all;
			}
			
			List<AdminLocation> list = get("0.0.0.0", loginName);
				
			if (list != null && list.size() > 0) {
				all.addAll(list);
			}
			
			return all;
		}
	}
}

