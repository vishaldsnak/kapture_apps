/**
 * 
 */
package com.webapp.mazda.assetstool.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.webapp.mazda.assetstool.logging.LogManager;
import com.webapp.mazda.assetstool.logging.Logger;

/**
 * @author darora
 * 
 */
public class ApplicationPropertiesUtil {
	static Logger log = LogManager.getLogger(ApplicationPropertiesUtil.class);

	public static String getProperty(String key) {
		String value = null;
		FileInputStream fis = null;
		try {
			Properties mainProperties = new Properties();
			String fileName = "application.properties";
			InputStream is = ApplicationPropertiesUtil.class.getClassLoader()
					.getResourceAsStream(fileName);

			mainProperties.load(is);
			value = mainProperties.getProperty(key);
			is.close();
			fileName = null;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			log.error(ioe.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return value;
	}

	public static String getProperty_BackUp(String key) {
		String value = null;
		FileInputStream fis = null;
		try {
			Properties mainProperties = new Properties();
			File jarPath = new File(ApplicationPropertiesUtil.class
					.getProtectionDomain().getCodeSource().getLocation()
					.getPath());

			//
			String propertiesPath = jarPath.getParentFile().getAbsolutePath();
			if (null != propertiesPath && !"".equals(propertiesPath)) {
				propertiesPath = propertiesPath.replace("\\", "/");
			}

			// String propertiesPath = ApplicationPropertiesUtil.class
			fis = new FileInputStream(propertiesPath
					+ "/application.properties");
			// fis = new FileInputStream(propertiesPath
			// + "application.properties");
			System.out.println("=============== Loading Props :: > "
					+ propertiesPath + "/application.properties");
			mainProperties.load(fis);
			value = mainProperties.getProperty(key);

		} catch (IOException ioe) {
			ioe.printStackTrace();
			log.error(ioe.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return value;
	}
}
