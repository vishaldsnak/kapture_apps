package com.mazda.gms3.mdm.logging;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

/**
 * This class will be responsible for creating the log and managing conditions
 * Just a small wrapper to the Log functionality to have centralized
 * functionality
 */
public class LogManager {

	private static boolean isLogPropReloading = false;

	static {
		LogManager.initLogger();
	}

	private static void initLogger() {
		try {
			LogManager.loadLog4J();
			// LogManager.printLog4jConfig();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized static void resetLog4j() {
		try {
			setLogPropReloading(true);
			URL fileUrl = searchFile();
			if (fileUrl != null) {
				org.apache.commons.logging.LogFactory.releaseAll();
				LogManager.loadLog4J();
			}
		} catch (Exception e) {
			setLogPropReloading(false);
			e.printStackTrace();
		}
		setLogPropReloading(false);
	}

	private synchronized static void loadLog4J() throws IOException {
		InputStream inputStream = LogManager.class.getClassLoader()
				.getResourceAsStream("log.properties");
		if (inputStream != null) {
			Properties properties = new Properties();
			properties.load(inputStream);
			PropertyConfigurator.configure(properties);
			System.out.println("Logging enabled...");
			inputStream.close();
		}
	}

	private final static URL searchFile() {
		URL fileURL = LogManager.class.getClassLoader().getResource(
				"log.properties");
		return fileURL;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.logging.LogFactory#getInstance(java.lang.Class)
	 */
	public static Logger getLogger(Class<?> clazz) {
		Logger logger = null;
		try {
			if (LogManager.isLogPropReloading()) {
				LogManager.class.wait();
			}
			logger = LogManager.getLog(clazz.getName());
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return logger;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.commons.logging.LogFactory#getInstance(java.lang.String)
	 */
	private static Logger getLog(String name) {
		// try {
		// LogFactory factory=LogFactory.getFactory();
		return (new Logger(name));
		// } catch (LogConfigurationException ex) {
		// ex.printStackTrace();
		// return new Logger(name);
		// }
	}

	/**
	 * @return Returns the isLogPropReloading.
	 */
	private static synchronized boolean isLogPropReloading() {
		return isLogPropReloading;
	}

	/**
	 * @param isLogPropReloading
	 *            The isLogPropReloading to set.
	 */
	private static synchronized void setLogPropReloading(
			boolean isLogPropertiesReloading) {
		LogManager.isLogPropReloading = isLogPropertiesReloading;
	}
}
