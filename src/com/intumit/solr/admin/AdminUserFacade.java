package com.intumit.solr.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpSession;

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

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.dataset.DataSet;
import com.intumit.solr.dataset.DataSetFacade;

import flexjson.JSONDeserializer;

public abstract class AdminUserFacade {
	public static final String SESSION_KEY = "_HHAUSK_";
	static AdminUserFacade instance = null;

	static final AdminUser EMPTY_ADMIN_USER = new AdminUser();

	/**
	 * Singleton
	 *
	 * @return
	 */
	public static AdminUserFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkAdminUserFacade();
			}
			else {
				instance = new DatabaseAdminUserFacade();
			}
		}
		return instance;
	}

	private AdminUserFacade() {
	}

	public abstract AdminUser login(String loginName, String password);

	public abstract void save(String adminUserName);

	public abstract void saveOrUpdate(AdminUser adminUser);

	public abstract void delete(int id) throws Exception;

	public abstract List<AdminUser> listAll();

	public abstract AdminUser get(int id);

	public abstract AdminUser getByLoginName(String loginName);
	
	public AdminUser getFromSession(HttpSession sess) {
		AdminUser admUser = (AdminUser)sess.getAttribute(AdminUserFacade.SESSION_KEY);
		return admUser;
	}

	public void setSession(HttpSession sess, AdminUser admUser) {
		boolean superAdmin = ("admin".equals(admUser.getLoginName()) && 1 == admUser.getId());
		if (superAdmin) {
			admUser.setSuperAdmin(true);	
		}
		sess.setAttribute(AdminUserFacade.SESSION_KEY, admUser);
		Collection<AdminGroup> groups = admUser.toAdminGroupCollection();
		Collection<Integer> groupIds = new ArrayList<Integer>();
		AdminGroup mergedAG = new AdminGroup();

		for (AdminGroup group: groups) {
			mergedAG.ORoperation(group);
			groupIds.add(group.getId());
		}
		AdminGroupFacade.getInstance().setSession(sess, mergedAG);
		AdminGroupFacade.getInstance().setGroupIdCollectionSession(sess, groupIds);
	}


	public static class ZkAdminUserFacade extends AdminUserFacade {

		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/admin/user";

		boolean cacheDirty = true;
		PathChildrenCache cache = null;

		public ZkAdminUserFacade() {
			CuratorFramework client = SearchManager.getCuratorClient();

			try {
				Stat stat = client.checkExists().forPath(MYPATH);
				if (stat == null) {
					client.create().creatingParentsIfNeeded().forPath(MYPATH + "/" + ID_SEED);
					DistributedAtomicInteger seed = new DistributedAtomicInteger(client, MYPATH + "/" + ID_SEED, new ExponentialBackoffRetry(1000, 3));
					seed.forceSet(0);


					AdminGroup firstGroup = new AdminGroup();
					firstGroup.setName("Administrator Group");
					firstGroup.setAdminAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
					firstGroup.setIndexAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
					firstGroup.setCoreAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
					firstGroup.setSystemAdminCURD(AdminGroup.SUPER_GROUP_PERM);
					firstGroup.setDataSetAdminCURD(AdminGroup.SUPER_GROUP_PERM);
					firstGroup.setStatisticsAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
					AdminGroupFacade.getInstance().saveOrUpdate(firstGroup);

					AdminGroup srmGroup = new AdminGroup();
					srmGroup.setName("SmartRobot Group");
					srmGroup.setSystemAdminCURD(AdminGroup.SUPER_GROUP_PERM);
					AdminGroupFacade.getInstance().saveOrUpdate(srmGroup);

					AdminUser firstAdmin = new AdminUser();
					firstAdmin.setLoginName("admin");
					firstAdmin.setName("ROOT");
					firstAdmin.setPassword(AdminUser.encryptPassword("intumit!!"));
					firstAdmin.setAdminGroups("" + AdminGroup.SUPER_GROUP_ID);
					saveOrUpdate(firstAdmin);

					AdminUser robotAdmin = new AdminUser();
					robotAdmin.setLoginName("srmadm");
					robotAdmin.setName("SRM ADM");
					robotAdmin.setPassword(AdminUser.encryptPassword("intumit"));
					robotAdmin.setAdminGroups("" + srmGroup.getId());
					saveOrUpdate(robotAdmin);


					DataSet defaultDs = new DataSet();
					defaultDs.setName("Default DataSet");
					defaultDs.setCoreName("core0");
					defaultDs.setDisplayRows("25");
					defaultDs.setQueryBoost("");
					defaultDs.setDsOrder(1L);
					defaultDs.setEnable(true);
					defaultDs.setVisible(true);
					defaultDs.setRss(false);
					defaultDs.setFieldWeight("Name_t^1 Description_mt^0.1");
					defaultDs.setFieldWikiCount(5L);
					defaultDs.setFieldMltCount(5L);

					DataSetFacade.getInstance().saveOrUpdate(defaultDs);
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
	                        System.out.println("AdminUser Node dirty, going to reload cache: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
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
		public AdminUser login(String loginName, String password) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}

				List<ChildData> children = cache.getCurrentData();
				String encryptedPwd = AdminUser.encryptPassword(password);

				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						AdminUser ds = new JSONDeserializer<AdminUser>().deserialize( jsonStr );

						if (ds.getLoginName().equals(loginName) && ds.getPassword().equals(encryptedPwd)) {
							return ds;
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void save(String adminUserName) {
			try {
				Integer intSeed = generateNewDsId();

				AdminUser adminUser = new AdminUser();
				adminUser.setId(intSeed);
				adminUser.setName(adminUserName);

				String dsZkPath = MYPATH + "/" + intSeed;

				SearchManager.getCuratorClient().create().forPath(dsZkPath, adminUser.serializeToJsonString().getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void saveOrUpdate(AdminUser adminUser) {
			try {
				if (adminUser.getId() == 0) {
					adminUser.setId(generateNewDsId());
				}
				String dsZkPath = MYPATH + "/" + adminUser.getId();
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);

				if (stat == null) {
					client.create().forPath(dsZkPath, adminUser.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, adminUser.serializeToJsonString().getBytes());
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
		public List<AdminUser> listAll() {


			List<AdminUser> list = new ArrayList<AdminUser>();
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}

				List<ChildData> children = cache.getCurrentData();

				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						AdminUser ds = new JSONDeserializer<AdminUser>().deserialize( jsonStr );
						list.add(ds);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		@Override
		public AdminUser get(int id) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				String dsZkPath = MYPATH + "/" + id;
				String jsonStr = new String(cache.getCurrentData(dsZkPath).getData());

				AdminUser ds = new JSONDeserializer<AdminUser>().deserialize( jsonStr );

				return ds;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		public AdminUser getByLoginName(String loginName) {
			if(StringUtils.isBlank(loginName)) {
				return null;
			}
			// 效能應該很差，找機會再改過
			List<AdminUser> allUsers = listAll();
			for(AdminUser user : allUsers) {
				if(StringUtils.equalsIgnoreCase(user.getLoginName(), loginName))
					return user;
			}
			return null;
		}


	}

	public static class DatabaseAdminUserFacade extends AdminUserFacade {

		public DatabaseAdminUserFacade() {
			if (listAll().size() == 0) {
				AdminGroup firstGroup = new AdminGroup();
				firstGroup.setName("Administrator Group");
				firstGroup.setAdminAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
				firstGroup.setIndexAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
				firstGroup.setCoreAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
				firstGroup.setSystemAdminCURD(AdminGroup.SUPER_GROUP_PERM);
				firstGroup.setDataSetAdminCURD(AdminGroup.SUPER_GROUP_PERM);
				firstGroup.setStatisticsAdminCURD(AdminGroup.C | AdminGroup.D | AdminGroup.R | AdminGroup.E1);
				AdminGroupFacade.getInstance().saveOrUpdate(firstGroup);

				AdminGroup srmGroup = new AdminGroup();
				srmGroup.setName("SmartRobot Group");
				srmGroup.setSystemAdminCURD(AdminGroup.SUPER_GROUP_PERM);
				AdminGroupFacade.getInstance().saveOrUpdate(srmGroup);

				AdminUser firstAdmin = new AdminUser();
				firstAdmin.setLoginName("admin");
				firstAdmin.setName("ROOT");
				firstAdmin.setPassword(AdminUser.encryptPassword("intumit!!"));
				firstAdmin.setAdminGroups("" + AdminGroup.SUPER_GROUP_ID);
				saveOrUpdate(firstAdmin);

				AdminUser robotAdmin = new AdminUser();
				robotAdmin.setLoginName("srmadm");
				robotAdmin.setName("SRM ADM");
				robotAdmin.setPassword(AdminUser.encryptPassword("intumit"));
				robotAdmin.setAdminGroups("" + srmGroup.getId());
				saveOrUpdate(robotAdmin);

				DataSet defaultDs = new DataSet();
				defaultDs.setName("Default DataSet");
				defaultDs.setCoreName("core0");
				defaultDs.setDisplayRows("25");
				defaultDs.setQueryBoost("");
				defaultDs.setDsOrder(1L);
				defaultDs.setEnable(true);
				defaultDs.setVisible(true);
				defaultDs.setRss(false);
				defaultDs.setFieldWeight("Name_t^1 Description_mt^0.1");
				defaultDs.setFieldWikiCount(5L);
				defaultDs.setFieldMltCount(5L);

				DataSetFacade.getInstance().saveOrUpdate(defaultDs);
			}
		}

		public AdminUser login(String loginName, String password) {
			List result = new ArrayList();

			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(AdminUser.class).addOrder(Order.asc("id"));
				ct.add( Restrictions.eq("loginName", loginName ) );
				ct.add( Restrictions.eq("password", AdminUser.encryptPassword(password) ) );
				ct.add(Restrictions.ne("disabled", true));
				result = ct.list();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}

			if (result.size() > 0) {
				return (AdminUser)result.get(0);
			}
			return null;
		}

		public synchronized void save(String adminUserName) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				AdminUser adminUser = new AdminUser();
				adminUser.setName(adminUserName);
				ses.saveOrUpdate(adminUser);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}

		public synchronized void saveOrUpdate(AdminUser adminUser) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.saveOrUpdate(adminUser);
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
				ses.createQuery("delete from " + AdminUser.class.getName() + " where id=" + id)
						.executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}

		public synchronized List<AdminUser> listAll() {
			return list();
		}

		public synchronized AdminUser get(int id) {
			try {
				return (AdminUser)HibernateUtil.getSession().get(AdminUser.class, id);
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
				Criteria ct = ses.createCriteria(AdminUser.class).addOrder(Order.asc("id"));
				ct.add(Restrictions.ne("disabled", true));
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
		public AdminUser getByLoginName(String loginName) {
			List<AdminUser> result = new ArrayList<AdminUser>();
			Session ses = null;
			try {
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(AdminUser.class).addOrder(Order.asc("id"));
				ct.add( Restrictions.eq("loginName", loginName ) );
				result = ct.list();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}

			if (result.size() > 0) {
				return (AdminUser)result.get(0);
			}
			return null;
		}
	}
}
