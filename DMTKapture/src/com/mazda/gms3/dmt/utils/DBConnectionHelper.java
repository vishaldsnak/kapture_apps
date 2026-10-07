package com.mazda.gms3.dmt.utils;

import java.sql.Connection;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;

public class DBConnectionHelper {
	static Logger logger = LogManager.getLogger(DBConnectionHelper.class);

	/**
	 * Prefix under which a servlet container publishes the resources declared for an
	 * application. WebLogic also resolves the bare resource name from the global tree,
	 * Tomcat does NOT - it only exposes the application's own resources below
	 * java:comp/env. The lookup below therefore tries the prefixed name first and falls
	 * back to the bare name, so the SAME code works on Tomcat and on WebLogic.
	 */
	private static final String ENV_PREFIX = "java:comp/env/";

	/** Cached DataSources - the JNDI lookup only needs to happen once per resource. */
	private static DataSource cachedDataSource = null;
	private static DataSource cachedCMSDataSource = null;

	/**
	 * Function will establish Connection with the conversion schema (kapture_dc) and
	 * will return connection Object.
	 *
	 * The DataSource is declared in the application's own META-INF/context.xml, so it is
	 * scoped to this application only.
	 *
	 * @return
	 */
	public static Connection getConnection()  {
		// a job that was asked to abort ends here - see ThreadAbortUtil
		ThreadAbortUtil.checkpoint();
		Connection connection = null;
		try {
			/*
			 * Load Database Connection using DataSource JNDI
			 */
			DataSource dataSource = getDataSource(ApplicationProperties
					.getProperty("jdbc.datasource"));
			connection = dataSource.getConnection();
			ensureBackslashIsOrdinaryCharacter(connection);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(DBConnectionHelper.class.getName(), "getConnection()", e);
			try {
				if (null != connection) {
					connection.close();
				}
			} catch (Exception ce) {
				// nothing more to do
			}
			// set connection to null
			connection = null;
		}
		return connection;
	}

	/**
	 * A BACKSLASH MUST BE AN ORDINARY CHARACTER ON EVERY CONVERSION CONNECTION.
	 *
	 * The DMT tables key every document on its source path, stored with backslashes, and the
	 * SQL joins those paths into its text. With MySQL's default mode a backslash is an escape
	 * and every lookup by path silently finds nothing - documents already loaded would be
	 * treated as new and withdrawn ones would never be found.
	 *
	 * context.xml asks for the mode through sessionVariables, but that depends on how the
	 * environment is set up (a changed url, a router or proxy in front of MySQL that hands the
	 * statement to another server session). So the mode is not assumed here: it is TESTED on
	 * every connection handed out, set when it is missing, and tested again. A connection on
	 * which it cannot be set is never handed out - the caller fails visibly instead of
	 * working on wrong answers.
	 */
	/**
	 * Physical connections already checked. A pooled connection keeps its session (and so its
	 * sql_mode) for its whole life, so each one is checked once instead of on every borrow -
	 * on MC Dev every statement costs a network round trip. Weak keys: a connection the pool
	 * closes simply drops out.
	 */
	private static final java.util.Map<Object, Boolean> checkedConnections =
			java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Object, Boolean>());

	private static Object physical(Connection connection) {
		try {
			return connection.unwrap(com.mysql.cj.jdbc.JdbcConnection.class);
		} catch (Exception e) {
			return null;
		}
	}

	private static void ensureBackslashIsOrdinaryCharacter(Connection connection) throws Exception {
		Object physical = physical(connection);
		if (null != physical && checkedConnections.containsKey(physical)) {
			return;
		}
		if (backslashIsOrdinaryCharacter(connection)) {
			if (null != physical) {
				checkedConnections.put(physical, Boolean.TRUE);
			}
			return;
		}
		java.sql.Statement stmt = connection.createStatement();
		try {
			stmt.execute("SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',NO_BACKSLASH_ESCAPES')");
		} finally {
			stmt.close();
		}
		if (!backslashIsOrdinaryCharacter(connection)) {
			throw new IllegalStateException("The database session does not keep sql_mode NO_BACKSLASH_ESCAPES."
					+ " No connection is handed out: document paths would not be found.");
		}
		if (null != physical) {
			checkedConnections.put(physical, Boolean.TRUE);
		}
		logger.info("ensureBackslashIsOrdinaryCharacter() :: sql_mode NO_BACKSLASH_ESCAPES was missing on a connection and has been set.");
	}

	/** Two backslashes between quotes are TWO characters only when a backslash is not an escape. */
	private static boolean backslashIsOrdinaryCharacter(Connection connection) throws Exception {
		java.sql.Statement stmt = connection.createStatement();
		java.sql.ResultSet rs = null;
		try {
			rs = stmt.executeQuery("SELECT LENGTH('\\\\')");
			return rs.next() && rs.getInt(1) == 2;
		} finally {
			if (null != rs) {
				rs.close();
			}
			stmt.close();
		}
	}

	/**
	 * Connection to the KAPTURE CMS database (kapture_cms_db) - the replacement for the
	 * Oracle OK_IM schema.
	 *
	 * This is a SEPARATE resource rather than a cross-database reference on the
	 * conversion connection because the two schemas are not always co-located. Tables
	 * are still written fully qualified ("kapture_cms_db.k_article"), so the resource's
	 * own default schema does not matter.
	 */
	public static Connection getCMSConnection()  {
		ThreadAbortUtil.checkpoint();
		Connection connection = null;
		try {
			DataSource dataSource = getCMSDataSource(ApplicationProperties
					.getProperty("jdbc.cms.datasource"));
			connection = dataSource.getConnection();
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(DBConnectionHelper.class.getName(), "getCMSConnection()", e);
			// set connection to null
			connection = null;
		}
		return connection;
	}

	/**
	 * Look the DataSource up once and remember it.
	 */
	private static synchronized DataSource getDataSource(String resourceName) throws Exception {
		if (null != cachedDataSource) {
			return cachedDataSource;
		}
		cachedDataSource = lookupDataSource(resourceName, "jdbc.datasource");
		return cachedDataSource;
	}

	/** Same lookup, cached separately for the CMS resource. */
	private static synchronized DataSource getCMSDataSource(String resourceName) throws Exception {
		if (null != cachedCMSDataSource) {
			return cachedCMSDataSource;
		}
		cachedCMSDataSource = lookupDataSource(resourceName, "jdbc.cms.datasource");
		return cachedCMSDataSource;
	}

	private static DataSource lookupDataSource(String resourceName, String propertyName)
			throws Exception {
		if (null == resourceName || "".equals(resourceName.trim())) {
			throw new IllegalStateException(propertyName
					+ " is not set in application.properties");
		}
		resourceName = resourceName.trim();

		Context ctx = new InitialContext();
		DataSource dataSource = null;
		try {
			// TOMCAT (and the JEE standard) - the application's own resources
			dataSource = (DataSource) ctx.lookup(resourceName.startsWith("java:")
					? resourceName : ENV_PREFIX + resourceName);
		} catch (Exception e) {
			// WEBLOGIC - resource published in the global JNDI tree under its bare name
			logger.info("lookupDataSource() :: " + ENV_PREFIX + resourceName
					+ " not found, retrying with the bare name :: >" + e.getMessage());
			dataSource = (DataSource) ctx.lookup(resourceName);
		}
		logger.info("lookupDataSource() :: DataSource resolved :: >" + resourceName);
		return dataSource;
	}
}
