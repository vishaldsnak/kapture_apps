/**
 *
 */
package com.mazda.gms3.dmt.utils;

import java.io.InputStream;
import java.util.Properties;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;



/**
 * @author darora
 *
 */
public class ApplicationProperties {
	static Logger log = LogManager.getLogger(ApplicationProperties.class);

	/**
	 * application.properties is packaged inside the WAR, so it cannot change while the
	 * application is running. It is therefore read ONCE and kept, instead of being
	 * re-opened and re-parsed on every getProperty() call (a conversion job makes
	 * thousands of them).
	 */
	private static volatile Properties cachedProperties = null;

	public static String getProperty(String key) {
		String value = null;
		try {
			value = load().getProperty(key);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		return value;
	}

	private static Properties load() throws Exception {
		Properties props = cachedProperties;
		if (null == props) {
			synchronized (ApplicationProperties.class) {
				props = cachedProperties;
				if (null == props) {
					props = new Properties();
					InputStream is = ApplicationProperties.class.getClassLoader()
							.getResourceAsStream("application.properties");
					try {
						props.load(is);
					} finally {
						if (null != is) {
							is.close();
						}
					}
					cachedProperties = props;
				}
			}
		}
		return props;
	}
}
