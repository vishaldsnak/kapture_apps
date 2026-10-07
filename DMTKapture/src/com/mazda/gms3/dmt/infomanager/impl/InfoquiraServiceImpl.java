/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import java.net.URL;

import com.inquira.imwows.generated.CategoryServices;
import com.inquira.imwows.generated.CategoryServicesServiceLocator;
import com.inquira.imwows.generated.ChannelServices;
import com.inquira.imwows.generated.ChannelServicesServiceLocator;
import com.inquira.imwows.generated.ContentServices;
import com.inquira.imwows.generated.ContentServicesServiceLocator;
import com.inquira.imwows.generated.SecurityServices;
import com.inquira.imwows.generated.SecurityServicesServiceLocator;
import com.inquira.imwows.generated.UserServices;
import com.inquira.imwows.generated.UserServicesServiceLocator;
import com.mazda.gms3.dmt.utils.ApplicationProperties;



/**
 * @author darora
 * 
 */
public class InfoquiraServiceImpl implements InfoquiraService {

	public SecurityServices getSecurityServices() throws Exception {
		SecurityServicesServiceLocator securityServiceLocator = new SecurityServicesServiceLocator();

		URL webserviceURL = new URL(ApplicationProperties.getProperty("SECURITY_SERVICE_ENDPOINT"));
		SecurityServices securityService = securityServiceLocator.getSecurityServices(webserviceURL);
		return securityService;
	}

	public ChannelServices getChannelServices() throws Exception {
		ChannelServicesServiceLocator channelServiceLocator = new ChannelServicesServiceLocator();
		URL webserviceURL = new URL(ApplicationProperties.getProperty("CHANNEL_SERVICE_ENDPOINT"));
		ChannelServices channelService = channelServiceLocator.getChannelServices(webserviceURL);
		return channelService;
	}

	
	public CategoryServices getCategoryServices() throws Exception {
		CategoryServicesServiceLocator categoryServiceLocator = new CategoryServicesServiceLocator();
		URL webserviceURL = new URL(ApplicationProperties.getProperty("CATEGORY_SERVICE_ENDPOINT"));
		CategoryServices categoryServices = categoryServiceLocator.getCategoryServices(webserviceURL);
		return categoryServices;
	}

	
	public ContentServices getContentServices() throws Exception {
		ContentServicesServiceLocator contentServicesServiceLocator = new ContentServicesServiceLocator();
		URL webserviceURL = new URL(ApplicationProperties.getProperty("CONTENT_SERVICE_ENDPOINT"));
		ContentServices contentServices = contentServicesServiceLocator.getContentServices(webserviceURL);
		return contentServices;
	}

	
	public UserServices getUserServices() throws Exception {
		UserServicesServiceLocator userServicesServiceLocator = new UserServicesServiceLocator();
		URL webserviceURL = new URL(ApplicationProperties.getProperty("USER_SERVICE_ENDPOINT"));
		UserServices userServices = userServicesServiceLocator.getUserServices(webserviceURL);
		return userServices;
	}

}
