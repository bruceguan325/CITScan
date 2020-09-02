package com.intumit.solr.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.solr.SearchManager;

import flexjson.JSONDeserializer;

public abstract class GroupDataSetFacade {
	static GroupDataSetFacade instance = null;
	
	public static GroupDataSetFacade getInstance() {
		if (instance == null) {
			if (SearchManager.isCloudMode()) {
				instance = new ZkGroupDataSetFacade();
			}
			else {
				instance = new DatabaseGroupDataSetFacade();
			}
		}
		return instance;
	}
	

	public abstract void saveOrUpdate(GroupDataSet gds);
	public abstract void delete(int adminGroupId) throws Exception;
	public abstract List<GroupDataSet> listAll(Collection<Integer> groupIds, int dsId);
	
	
	public static class ZkGroupDataSetFacade extends GroupDataSetFacade {

		final static String ID_SEED = "seed";
		final static String MYPATH = "/wise/admin/groupDatasetAcl";

		public ZkGroupDataSetFacade() {
			super();
			CuratorFramework client = SearchManager.getCuratorClient();
			
			try {
				Stat stat = client.checkExists().forPath(MYPATH);
				if (stat == null) {
					client.create().creatingParentsIfNeeded().forPath(MYPATH + "/" + ID_SEED);
					DistributedAtomicInteger seed = new DistributedAtomicInteger(client, MYPATH + "/" + ID_SEED, new ExponentialBackoffRetry(1000, 3));
					seed.forceSet(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		public void saveOrUpdate(GroupDataSet gds) {
			try {
				if (gds.getId() == 0) {
					gds.setId(generateNewDsId());
				}
				String dsZkPath = MYPATH + "/" + gds.getId();
				CuratorFramework client = SearchManager.getCuratorClient();
				Stat stat = client.checkExists().forPath(dsZkPath);
				
				if (stat == null) {
					client.create().forPath(dsZkPath, gds.serializeToJsonString().getBytes());
				}
				else {
					client.setData().forPath(dsZkPath, gds.serializeToJsonString().getBytes());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void delete(int adminGroupId) throws Exception {
			String dsZkPath = MYPATH + "/" + adminGroupId;
			CuratorFramework client = SearchManager.getCuratorClient();
			Stat stat = client.checkExists().forPath(dsZkPath);
			
			if (stat != null) {
				client.delete().forPath(dsZkPath);
			}
		}

		@Override
		public List<GroupDataSet> listAll(Collection<Integer> groupIds, int dsId) {
			List<GroupDataSet> list = new ArrayList<GroupDataSet>();
			try {
				CuratorFramework client = SearchManager.getCuratorClient();
				List<String> gdsIds = client.getChildren().forPath(MYPATH);
				
				
				for (String gdsId: gdsIds) {
					if (!gdsId.equals(ID_SEED)) {
						String fullpath = MYPATH + "/" + gdsId;
						String jsonStr = new String(client.getData().forPath(fullpath));
						GroupDataSet gds = new JSONDeserializer<GroupDataSet>().deserialize( jsonStr );
						
						if (dsId == gds.getDataSetId() && groupIds.contains(gds.getAdminGroupId()))
								list.add(gds);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}
	}
	
	public static class DatabaseGroupDataSetFacade extends GroupDataSetFacade {

		@Override
		public void saveOrUpdate(GroupDataSet gds) {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.saveOrUpdate(gds);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}

		@Override
		public void delete(int adminGroupId) throws Exception {
			Session ses = null;
			Transaction tx = null;
			try {
				ses = HibernateUtil.getSession();
				tx = ses.beginTransaction();
				ses.createQuery(
						"delete from " + GroupDataSet.class.getName()
								+ " where groupId=" + adminGroupId).executeUpdate();
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			} finally {
				ses.close();
			}
		}

		@Override
		public List<GroupDataSet> listAll(Collection<Integer> groupIds, int dataSetId) {
			Session ses = null;
			try {
				List result = new ArrayList();
				ses = HibernateUtil.getSession();
				Criteria ct = ses.createCriteria(GroupDataSet.class).addOrder(Order.asc("id"));
				ct.add(Restrictions.in("groupId", groupIds));
				ct.add(Restrictions.eq("dataSetId", new Integer(dataSetId)));
				result = ct.list();
				
				return result;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				ses.close();
			}
			return null;
		}
		
	}
}
