package com.intumit.solr;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Session;

import com.intumit.hibernate.HibernateUtil;

public class SearchStatTest {

	public static void main(String[] args) {
		HibernateUtil.init();

		Connection con = null;
		PreparedStatement pstmt = null;
		Session ses = null;
		try {
			ses = HibernateUtil.getSession();
			con = ses.connection();
			con.setAutoCommit(false);

			String sql = "SELECT * FROM SearchKeywordLog WHERE logtime >= ? AND logtime < ?";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, "2012-10-01 00:00:00");
			pstmt.setString(2, "2012-11-01 00:00:00");
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				System.out.println(rs.getString("name"));
			}

		} catch (SQLException e) {
			try {
				if (con != null)
					con.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
}
