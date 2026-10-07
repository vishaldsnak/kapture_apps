/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import com.inquira.imwows.generated.CategoryServices;
import com.inquira.imwows.generated.ChannelServices;
import com.inquira.imwows.generated.ContentServices;
import com.inquira.imwows.generated.SecurityServices;
import com.inquira.imwows.generated.UserServices;


/**
 * @author darora
 *
 */
public interface InfoquiraService {
	
	public SecurityServices getSecurityServices() throws Exception;
	
	public ChannelServices getChannelServices() throws Exception;
	
	public CategoryServices getCategoryServices() throws Exception;
	
	public ContentServices getContentServices() throws Exception;
	
	public UserServices getUserServices() throws Exception;
	
	
	
}
