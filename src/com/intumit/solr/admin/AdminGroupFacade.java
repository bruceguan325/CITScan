package com.intumit.solr.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.intumit.solr.tenant.TenantAdminGroup;

import flexjson.JSONDeserializer;

public abstract class AdminGroupFacade {
	public static final String SESSION_KEY = "_HHAGSK_";
	public static final String GROUP_ID_COLLECTION_SESSION_KEY = "_HHAGIDCSK_";

	static AdminGroupFacade instance = null;
	static final AdminGroup EMPTY_ADMIN = new AdminGroup();

	/**
	 * Singleton
	 *
	 * @return
	 */
	public static AdminGroupFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkAdminGroupFacade();
			}
			else {
				instance = new DatabaseAdminGroupFacade();
			}
		}
		return instance;
	}

	public abstract void save(String adminGroupName);

	public abstract void saveOrUpdate(AdminGroup adminGroup);

	public abstract void saveOrUpdate(AdminGroup adminGroup, List<GroupDataSet> grpDataSetList);

	public abstract void delete(int adminGroupId) throws Exception;

	public abstract List<AdminGroup> listAll();

	public abstract AdminGroup get(int adminGroupId);

	public abstract int getDataSetViewAcl(HttpSession sess, int dataSetId);

	public abstract int getDataSetViewAcl(Collection<Integer> adminGroupIds, int dataSetId);

	public abstract AdminGroup getByName(String adminGroupName);

	public AdminGroup getFromSession(HttpSession sess) {
		TenantAdminGroup tag = TenantAdminGroup.getFromSession(sess);
		AdminGroup admGrp = null;
		
		// 如果有 Tenant 專屬設定，則強制使用該 AdminGroup 
		if (tag != null) {
			admGrp = tag.getAdminGroup();
			admGrp.ORoperation2(admGrp);
		}
		
		if (admGrp == null) {
			admGrp = (AdminGroup)sess.getAttribute(AdminGroupFacade.SESSION_KEY);
		}
		
		if (admGrp == null) {
			return EMPTY_ADMIN;
		}
		
		return admGrp;
	}

	public void setSession(HttpSession sess, AdminGroup admGrp) {
		sess.setAttribute(AdminGroupFacade.SESSION_KEY, admGrp);
	}

	protected void setGroupIdCollectionSession(HttpSession sess,
			Collection<Integer> groupIds) {
		sess.setAttribute(AdminGroupFacade.GROUP_ID_COLLECTION_SESSION_KEY, groupIds);
	}


	public static class ZkAdminGroupFacade extends AdminGroupFacade {

		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/admin/group";

		boolean cacheDirty = true;
		PathChildrenCache cache = null;

		public ZkAdminGroupFacade() {
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
	                        System.out.println("AdminGroup Node dirty, going to reload cache: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
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
		public void save(String adminGroupName) {
			try {
				Integer intSeed = generateNewDsId();

				AdminGroup adminGroup = new AdminGroup();
				adminGroup.setId(intSeed);
				adminGroup.setName(adminGroupName);

				String dsZkPath = MYPATH + "/" + intSeed;

				SearchManager.getCuratorClient().create().forPath(dsZkPath, adminGroup.serializeToJsonString().getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void saveOrUpdate(AdminGroup adminGroup) {
			try {
				if (adminGroup.getId() == 0) {
					adminGroup.setId(generateNewDsId());
				}
				String dsZkPath = MYPATH + "/" + adminGroup.getId();
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);

				if (stat == null) {
					client.create().forPath(dsZkPath, adminGroup.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, adminGroup.serializeToJsonString().getBytes());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void saveOrUpdate(AdminGroup adminGroup, List<GroupDataSet> grpDataSetList) {

			try {
				if (adminGroup.getId() == 0) {
					adminGroup.setId(generateNewDsId());
				}
				String dsZkPath = MYPATH + "/" + adminGroup.getId();
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);

				if (stat == null) {
					client.create().forPath(dsZkPath, adminGroup.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, adminGroup.serializeToJsonString().getBytes());
				}

				GroupDataSetFacade gdsf = GroupDataSetFacade.getInstance();
				gdsf.delete(adminGroup.getId());
				for (int i = 0; i < grpDataSetList.size(); i++) {
					GroupDataSet gds = grpDataSetList.get(i);
					gds.setAdminGroupId(adminGroup.getId());
					gdsf.saveOrUpdate(gds);
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
			GroupDataSetFacade.getInstance().delete(id);
		}

		@Override
		public List<AdminGroup> listAll() {

			List<AdminGroup> list = new ArrayList<AdminGroup>();
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}

				List<ChildData> children = cache.getCurrentData();

				for (ChildData child: children) {
					if (!child.getPath().equals(MYPATH + "/" + ID_SEED)) {
						String jsonStr = new String(child.getData());
						AdminGroup ds = new JSONDeserializer<AdminGroup>().deserialize( jsonStr );
						list.add(ds);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		@Override
		public AdminGroup get(int id) {
			try {
				if (cacheDirty) {
					cache.rebuild();
					cacheDirty = false;
				}
				String dsZkPath = MYPATH + "/" + id;
				String jsonStr = new String(cache.getCurrentData(dsZkPath).getData());

				AdminGroup ds = new JSONDeserializer<AdminGroup>().deserialize( jsonStr );

				return ds;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		public int getDataSetViewAcl(HttpSession sess, int dataSetId) {
			Collection<Integer> groupIds = (Collection<Integer>)sess.getAttribute(AdminGroupFacade.GROUP_ID_COLLECTION_SESSION_KEY);
			return getDataSetViewAcl(groupIds, dataSetId);
		}

		@Override
		public int getDataSetViewAcl(Collection<Integer> adminGroupIds, int dataSetId) {

			if (adminGroupIds.contains(AdminGroup.SUPER_GROUP_ID)) {
				return AdminGroup.SUPER_GROUP_PERM;
			}

			int mergedPerm = 0;
			try {
				List<GroupDataSet> result = GroupDataSetFacade.getInstance().listAll(adminGroupIds, dataSetId);
				for (int i=0; i < result.size(); i++) {
					mergedPerm |= ((GroupDataSet)result.get(i)).getDataSetViewAdminCURD();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return mergedPerm;
		}

		@Override
		public AdminGroup getByName(String adminGroupName) {
			if(StringUtils.isBlank(adminGroupName)) return null;
			List<AdminGroup> allGroups = listAll();
			for(AdminGroup g : allGroups) {
				if(StringUtils.equalsIgnoreCase(adminGroupName, g.getName()))
					return g;
			}
			return null;
		}


	}


	public static class DatabaseAdminGroupFacade extends AdminGroupFacade {

		public synchronized void save(String adminGroupName) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				AdminGroup adminGroup = new AdminGroup();
				adminGroup.setName(adminGroupName);
				ses.saveOrUpdate(adminGroup);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}

		public synchronized void saveOrUpdate(AdminGroup adminGroup) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.saveOrUpdate(adminGroup);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}

		public synchronized void saveOrUpdate(AdminGroup adminGroup, List<GroupDataSet> grpDataSetList) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();

				GroupDataSetFacade gdsf = GroupDataSetFacade.getInstance();
				gdsf.delete(adminGroup.getId());
				for (int i = 0; i < grpDataSetList.size(); i++) {
					gdsf.saveOrUpdate(grpDataSetList.get(i));
				}

				ses.saveOrUpdate(adminGroup);
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
				GroupDataSetFacade.getInstance().delete(id);
				ses.createQuery(
						"delete from " + AdminGroup.class.getName()
								+ " where id=" + id).executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}

		public synchronized List<AdminGroup> listAll() {
			return list();
		}

		public synchronized AdminGroup get(int id) {
			try {
				return (AdminGroup) HibernateUtil.getSession().get(AdminGroup.class, id);
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
				Criteria ct = ses.createCriteria(AdminGroup.class).addOrder(
						Order.asc("id"));
				// if (xdmpId != null)
				// ct.add( Restrictions.eq("xdmp", xdmpId ) );
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

/*
		public AdminGroup login(String id) {
			if(id == null)
				return null;
			List result = new ArrayList();

			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				Criteria ct = ses.createCriteria(AdminGroup.class).addOrder(Order.asc("id"));
				ct.add( Restrictions.eq("id", new Integer(id)));
				result = ct.list();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}

			if (result.size() > 0) {
				return (AdminGroup)result.get(0);
			}
			return null;
		}*/


		public int getDataSetViewAcl(Collection<Integer> groupIds, int dataSetId) {
			if (groupIds.contains(AdminGroup.SUPER_GROUP_ID)) {
				return AdminGroup.SUPER_GROUP_PERM;
			}

			int mergedPerm = 0;
			Session ses = null;
			try {
				List result = new ArrayList();
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(GroupDataSet.class).addOrder(Order.asc("id"));
				ct.add( Restrictions.in("groupId", groupIds));
				ct.add( Restrictions.eq("dataSetId", new Integer(dataSetId )));
				result = ct.list();

				for (int i=0; i < result.size(); i++) {
					mergedPerm |= ((GroupDataSet)result.get(i)).getDataSetViewAdminCURD();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}
			return mergedPerm;
		}

		public int getDataSetViewAcl(HttpSession sess, int dataSetId) {
			Collection<Integer> groupIds = (Collection<Integer>)sess.getAttribute(AdminGroupFacade.GROUP_ID_COLLECTION_SESSION_KEY);
			return getDataSetViewAcl(groupIds, dataSetId);
		}

		@Override
		public AdminGroup getByName(String adminGroupName) {
			List<AdminGroup> result = new ArrayList<AdminGroup>();
			Session ses = null;
			try {
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(AdminGroup.class).addOrder(Order.asc("id"));
				ct.add( Restrictions.eq("name", adminGroupName ) );
				result = ct.list();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}

			if (result.size() > 0) {
				return (AdminGroup)result.get(0);
			}
			return null;
		}
	}
	
	public static String findAuditorByQaCategory(String qaCategory, String tid)
			throws Exception, SQLException {
		Connection con = null;
		PreparedStatement pstmt = null;
		
		Session ses = null;
		String email = "";
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);
			String sql = "";
			
			sql = "SELECT * FROM AdminUser where adminGroups in (SELECT id FROM AdminGroup where (systemAdminCURD & 0x10000 > 0)) and (department = '*'";
			if(qaCategory.equals("通用")) {
				sql += ")";
			} else {
				sql += " or department = '"+qaCategory+"')";
			}

			pstmt = con.prepareStatement(sql);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				if(!email.equals(""))
					email += ",";
				String[] tenantIds = rs.getString("tenantIds").split(",");
				if(Arrays.asList(tenantIds).contains(tid))
					email += rs.getString("email");
			}
			return email;

		} catch (SQLException e) {
			if (con != null)
				con.rollback();
			e.printStackTrace();
			throw e;
		} finally {
			ses.close();
		}

	}


}
