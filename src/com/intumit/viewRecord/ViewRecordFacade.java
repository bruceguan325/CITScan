package com.intumit.viewRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.intumit.hibernate.HibernateUtil;

public class ViewRecordFacade {

	public static synchronized ViewRecordEntity getByKeyword(String keyword) {
		List result = new ArrayList();

		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			Criteria ct = ses.createCriteria(ViewRecordEntity.class);
			ct.add(Restrictions.eq("keyword", keyword));

			result = ct.setFetchSize(1).list();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}

		return result.size() > 0 ? (ViewRecordEntity) result.get(0) : null;
	}

	public static synchronized void save(ViewRecordEntity viewRecord) {
		Session ses = null;
		Transaction tx = null;
		try {
			ses = HibernateUtil.getSession();
			tx = ses.beginTransaction();
			ses.saveOrUpdate(viewRecord);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
		} finally {
			ses.close();
		}
	}

	public static void addIdRecords(String keyword, String id)
			throws Exception, SQLException {
		Connection con = null;
		PreparedStatement pstmt = null;
		PreparedStatement pstmtDel = null;
		PreparedStatement pstmtUpd = null;
		int viewTime = 0;
		boolean haveRecord = false;
		int viewRecordEntity_id = 0;

		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);

			String sql = "select * from KeywordToIdRecord kId "
					+ "join ViewRecordEntity vRecord on vRecord.id = kId.viewRecordEntity_id "
					+ "where vRecord.keyword=? and kId.idRecord=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, keyword);
			pstmt.setString(2, id);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				viewTime = rs.getInt("viewTime");
				viewRecordEntity_id = rs.getInt("viewRecordEntity_id");
				haveRecord = true;
			}

			if (!haveRecord) {
				viewRecordEntity_id = checkViewRecordId(keyword);
				if (viewRecordEntity_id == -1)
					return;
				String sqlUpd = "insert into KeywordToIdRecord"
						+ " (idRecord, viewTime, viewRecordEntity_id, modifyTime)"
						+ " values(?,?,?,?)";
				pstmtUpd = con.prepareStatement(sqlUpd);
				pstmtUpd.setString(1, id);
				pstmtUpd.setInt(2, 1);
				pstmtUpd.setInt(3, viewRecordEntity_id);
				pstmtUpd.setTimestamp(4, new Timestamp(Calendar.getInstance()
						.getTime().getTime()));
				pstmtUpd.executeUpdate();
				con.commit();
			} else {
				String sqlUpd = "UPDATE KeywordToIdRecord SET viewTime=?, modifyTime=?"
						+ "  WHERE idRecord=? and viewRecordEntity_id=?";
				pstmtUpd = con.prepareStatement(sqlUpd);
				pstmtUpd.setInt(1, viewTime + 1);
				pstmtUpd.setTimestamp(2, new Timestamp(Calendar.getInstance()
						.getTime().getTime()));
				pstmtUpd.setString(3, id);
				pstmtUpd.setInt(4, viewRecordEntity_id);
				pstmtUpd.executeUpdate();
				con.commit();
			}

		} catch (SQLException e) {
			if (con != null)
				con.rollback();
			e.printStackTrace();
			throw e;
		} finally {
			ses.close();
		}

	}

	public static int checkViewRecordId(String keyword) throws Exception,
			SQLException {
		Connection con = null;
		PreparedStatement pstmt = null;
		PreparedStatement pstmtUpd = null;
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);

			String sql = "select * from ViewRecordEntity where keyword=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, keyword);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				return rs.getInt("id");
			}

			String sqlUpd = "insert into ViewRecordEntity" + " (keyword)"
					+ " values(?)";
			pstmtUpd = con.prepareStatement(sqlUpd);
			pstmtUpd.setString(1, keyword);
			pstmtUpd.executeUpdate();
			con.commit();

			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, keyword);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				return rs.getInt("id");
			}

		} catch (SQLException e) {
			if (con != null)
				con.rollback();
			e.printStackTrace();
			throw e;
		} finally {
			ses.close();
		}

		return -1;
	}

	public static List<String> getTopViewRecordId(String keyword, int limit) throws Exception {
		List<String> topViewList = new ArrayList<String>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			StringBuffer hql = new StringBuffer(" from ");
			hql.append(KeywordToIdRecord.class.getName()).append(" kId ");
			hql.append(" where kId.viewRecordEntity.keyword = :keyword order by kId.viewTime desc");
			Query q = ses.createQuery(hql.toString());
			q.setString("keyword", keyword);
			q.setMaxResults(limit);
			List<KeywordToIdRecord> result = q.list();
			for(KeywordToIdRecord record : result) {
				topViewList.add(record.getIdRecord());
			}

		} catch (HibernateException e) {
			e.printStackTrace();
			throw e;
		} finally {
			ses.close();
		}
		return topViewList;
	}

	public static List<ViewStatisticsEntity> viewRecordStatistics(
			String keyword, int start, int limit) throws Exception {
		List<ViewStatisticsEntity> statisticsList = new ArrayList<ViewStatisticsEntity>();
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			StringBuffer hql = new StringBuffer(" from ");
			hql.append(KeywordToIdRecord.class.getName()).append(" kId ");
			hql.append(" where kId.viewRecordEntity.keyword = :keyword order by kId.viewTime desc");
			Query q = ses.createQuery(hql.toString());
			q.setString("keyword", keyword);
			q.setFirstResult(start);
			q.setMaxResults(limit);
			List<KeywordToIdRecord> result = q.list();

			for (KeywordToIdRecord kId : result) {
				ViewStatisticsEntity ob = new ViewStatisticsEntity();
				ob.setIdRecord(kId.getIdRecord());
				ob.setViewTime(kId.getViewTime());
				statisticsList.add(ob);
			}
		} catch (HibernateException e) {
			e.printStackTrace();
			throw e;
		} finally {
			ses.close();
		}
		return statisticsList;
	}

	public static int viewRecordStatisticsCount(String keyword)
			throws Exception, SQLException {
		Connection con = null;
		PreparedStatement pstmt = null;
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);

			String sql = "select count(*) from KeywordToIdRecord kId "
					+ "join ViewRecordEntity vRecord on vRecord.id = kId.viewRecordEntity_id "
					+ "where vRecord.keyword=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, keyword);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				return rs.getInt(1);
			}

		} catch (SQLException e) {
			if (con != null)
				con.rollback();
			e.printStackTrace();
			throw e;
		} finally {
			ses.close();
		}

		return -1;
	}

}
