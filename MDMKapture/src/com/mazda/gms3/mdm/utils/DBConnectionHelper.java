package com.mazda.gms3.mdm.utils;

import java.sql.Connection;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;

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
	 * Function will establish Connection with Local Database and will return
	 * connection Object.
	 *
	 * The DataSource is declared in the application's own META-INF/context.xml, so it is
	 * scoped to this application only - no server-wide configuration is involved and no
	 * other application deployed on the same server is affected.
	 *
	 * @return
	 */
	public static Connection getConnection() throws Exception {
		Connection connection = null;
		try {
			/*
			 * Load Database Connection using DataSource JNDI
			 */
			// connection = CommonDatabaseUtility.getMS3Connection();
			DataSource dataSource = getDataSource(ApplicationProperties
					.getProperty("jdbc.datasource"));
			connection = dataSource.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("getConnection() :: Exception :: >" + e.getMessage());
			// set connection to null
			connection = null;
		}
		return connection;
	}

	/**
	 * Connection to the KAPTURE CMS database (kapture_cms_db) - the replacement for the
	 * Oracle OK_IM schema.
	 *
	 * WHY THIS IS A SEPARATE DATASOURCE AND NOT A CROSS-DATABASE QUERY ON getConnection():
	 * on the VDI server kapture_dc and kapture_cms_db happen to live on the SAME MySQL
	 * server, so a "kapture_cms_db.<table>" reference would resolve over the conv
	 * connection. On a developer workstation they do NOT - kapture_dc is local while
	 * kapture_cms_db is the shared AWS-hosted server. A dedicated resource is therefore the
	 * only form that works in BOTH environments; where they are co-located the two
	 * resources simply point at the same server.
	 *
	 * Tables are still written fully qualified ("kapture_cms_db.k_article"), so the
	 * resource's own default schema does not matter.
	 */
	public static Connection getCMSConnection() throws Exception {
		Connection connection = null;
		try {
			DataSource dataSource = getCMSDataSource(ApplicationProperties
					.getProperty("jdbc.cms.datasource"));
			connection = dataSource.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("getCMSConnection() :: Exception :: >" + e.getMessage());
			// set connection to null
			connection = null;
		}
		return connection;
	}

	/**
	 * Look the DataSource up once and remember it. Both the container-scoped name
	 * (java:comp/env/<name>) and the bare name are tried so that the application runs
	 * unchanged on Tomcat and on WebLogic.
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
