package com.mazda.gms3.mdm.utils;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Properties;




/**
 * Provides a centralized repository for all configuration information.
 * <p>
 * All configuration information should be retrieved here for every use. The
 * MessagePropertyFileUpdateListener will reload configuration changes
 * dynamically.
 * <p>
 * Classes may cache the reference to the ApplicationProperties instance, as
 * only one will ever be created.
 * 
 * 
 */
public class MessageProperties 
{
	/**
	 * holds the last updated Message properties
	 */
	private static Properties properties = new Properties();

	private static String MESSAGE_PROPERTIES = "/com/mazda/gms3/mdm/properties/nl/";
	
	private static String ENGLISH_LOCALE_FILE="mdmresource_en.properties";
	
	private static String JAPANESE_LOCALE_FILE="mdmresource_jp.properties";
	
	/**
	 * C'tor: restricts the object instantiation for this class
	 */
	public MessageProperties(Object locale)
	{
		try 
		{
			/**
			 * Load the properties from disk.
			 * 
			 */
			MessageProperties.loadProperty(locale);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			// System.out.println("Message Properties :: Exception :: Constructor() :: => "+e.getMessage());  
		}
	}


	/**
	 * Get the value of a property.
	 * 
	 * @param propertyName
	 * @return
	 */
	public String getProperty(String propertyName) 
	{		
		String returnValue = null;
		returnValue = MessageProperties.properties.getProperty(propertyName);
		if (returnValue != null )
		{
			returnValue.trim();
		}
		return returnValue;
	}


	/**
	 * loads property from properties file and store to cache.
	 * sync
	 * Identify the locale in session and prepare the properties file path for it.
	 * 
	 * 	if the locale from session is null - 
	 * 		then consider the default locale for preparing properties file path.
	 * 
	 * 	The properties file path will be  - 
	 * 		IISA Framework Working Directory + Configuration Directory + Properties Directory + Message Constants_<localValue>.properties
	 */

	public static synchronized void loadProperty(Object locale)
	{
		try 
		{
			String propertiesPath="";
			if(null!=locale && !"".equals(locale))
			{
				if(locale.toString().equals(ApplicationProperties.getProperty("locales.values.english")))
				{
					propertiesPath = MESSAGE_PROPERTIES+ENGLISH_LOCALE_FILE;
				}
				else if(locale.toString().equals(ApplicationProperties.getProperty("locales.values.japanese")))
				{
					propertiesPath = MESSAGE_PROPERTIES+JAPANESE_LOCALE_FILE;
				}
				else
				{
					// set default Locale english
					propertiesPath = MESSAGE_PROPERTIES+ENGLISH_LOCALE_FILE;
				}
			}
			else
			{
				// set default Locale english
				propertiesPath = MESSAGE_PROPERTIES+ENGLISH_LOCALE_FILE;
			}
			
			/**
			 * Start Creating File Path  -
			 *  Check for local value in session
			 */
			
//			String propertiesFilePath = "";
//			File propsFile = new File(propertiesFilePath);
			InputStream inputStream  = MessageProperties.class.getResourceAsStream(propertiesPath);
//			InputStream inputStream = new FileInputStream(propsFile);
			if(inputStream!=null)
			{
				MessageProperties.properties.load(inputStream);
				inputStream.close();
			}
			
			// set propsFile to null
//			propsFile=null;
//			localeValue = null;
//			propertiesFilePath = null;
//			obj = null;
			propertiesPath = null;
		} 
		catch (IOException e)
		{
			e.printStackTrace();
			 System.out.println("Message Properties :: Exception :: loadProperty()  : : =>  "+e.getMessage()); 
		} catch (Exception e) 
		{
			e.printStackTrace();
			 System.out.println("Message Properties :: Exception :: loadProperty()  : : =>  "+e.getMessage());
		}
	}

	/**
	 * 
	 * @param propertyName
	 * @param val
	 * @return
	 */
	public String getProperty(String propertyName,String val)
	{		
		String returnValue = null;
		returnValue = MessageProperties.properties.getProperty(propertyName,val);     
		if (returnValue != null )
		{
			returnValue.trim();
		}
		return returnValue;
	}
	
	public String getMessage(String[] id, String messageKey) {
		try {
			String resource = getProperty(messageKey);
			if (id != null) {
				String []argument =new String[id.length];
				for(int i=0; i<id.length; i++){
				argument[i] = id[i];
//				System.out.println("Argument value from bundle :: " + argument[i]);
				}	
				MessageFormat formatter = new MessageFormat(resource);
				return formatter.format(argument);
			}		
			return resource;
		} catch (MissingResourceException e) {
			e.printStackTrace();
		}
		return null;
	}
	

	
	public String addMessage(String messageKey, String key)
	{
		String message="";
		try
		{
			if(null!=key && !"".equals(key))
			{
				message = getMessage(new String[]{key}, messageKey);
			}
			else
			{
				message = getMessage(null, messageKey);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return message;
	}
	
	public String addMessage(String messageKey, String key1, String key2)
	{
		String message="";
		try
		{
			if(null!=key1 && !"".equals(key1) && null!=key2 && !"".equals(key2))
			{
				message = getMessage(new String[]{key1,key2}, messageKey);
			}
			else
			{
				message = getMessage(null, messageKey);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return message;
	}
	
	

}