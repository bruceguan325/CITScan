package com.intumit.solr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class HsqlDb {
	private static final String dbString = "jdbc:hsqldb:hsql://localhost:9999/solrdb";
	private static final String dbPath = "db/solr/solrdb";

	static {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (Exception e) {
			System.out.println("ERROR: failed to load HSQLDB JDBC driver.");
			e.printStackTrace();
		}
	}

	public static String getDbString() {
		return dbString;
	}

	public static String getDbPath() {
		return dbPath;
	}

	public static Connection getConnection() {
		try {
			Connection c = DriverManager.getConnection(getDbString(), "sa", "");
			c.setAutoCommit(false);
			return c;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static Connection getTestConnection() {
		try {
			Connection c = DriverManager.getConnection(
					"jdbc:hsqldb:file:db/test/solrdb", "sa", "");
			c.setAutoCommit(false);
			return c;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
