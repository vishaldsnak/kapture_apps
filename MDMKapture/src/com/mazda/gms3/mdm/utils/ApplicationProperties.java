/**
 * 
 */
package com.mazda.gms3.mdm.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;


/**
 * @author darora
 * 
 */
public class ApplicationProperties {
	static Logger log = LogManager.getLogger(ApplicationProperties.class);

	public static String getProperty(String key) {
		String value = null;
		FileInputStream fis = null;
		try {
			Properties mainProperties = new Properties();
			String fileName = "application.properties";
			InputStream is = ApplicationProperties.class.getClassLoader()
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
}
