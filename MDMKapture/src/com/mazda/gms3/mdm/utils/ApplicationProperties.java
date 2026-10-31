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

	/** A REQUIRED value - no default in code: a missing or blank key throws. Returned trimmed. */
	public static String requireProperty(String key) {
		String value = getProperty(key);
		if (null == value || "".equals(value.trim())) {
			throw new IllegalStateException("application.properties :: required key {" + key + "} is missing or blank.");
		}
		return value.trim();
	}

	/**
	 * A REQUIRED whole number of at least 1 - no default in code: a missing, blank, non-numeric or
	 * zero / negative value throws, so a wrong env file stops the step instead of running on a guess.
	 */
	public static int requirePositiveInt(String key) {
		String value = getProperty(key);
		if (null == value || "".equals(value.trim())) {
			throw new IllegalStateException("application.properties :: required key {" + key + "} is missing or blank.");
		}
		int number;
		try {
			number = Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			throw new IllegalStateException("application.properties :: key {" + key + "} = {" + value + "} is not a whole number.");
		}
		if (number < 1) {
			throw new IllegalStateException("application.properties :: key {" + key + "} = {" + value + "} must be 1 or more.");
		}
		return number;
	}
}
