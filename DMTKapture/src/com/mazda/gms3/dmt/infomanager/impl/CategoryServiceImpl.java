/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import org.apache.log4j.Logger;

/**
 * @author darora
 *
 */
public class CategoryServiceImpl{

	static Logger logger = Logger.getLogger(CategoryServiceImpl.class);
	
	public String getCategory(String token, String inputXML) {
		String category = null;
		try{
			// Create object of InfoQuiraServiceImpl class and call getCategory() function
			InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
			category = infoquiraServiceImpl.getCategoryServices().getCategory(token, inputXML);
		} catch(Exception exception){
			logger.error("Get Category :: Exception :: >" + exception.getMessage());
			exception.printStackTrace();
		}
		return category;
	}
	
	

}
