/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import org.apache.log4j.Logger;

/**
 * @author darora
 * 
 */
public class UserServiceImpl {
	static Logger log = Logger.getLogger(UserServiceImpl.class);
	
	
	public String getUserForLogin(String userLoginXML, String token) {
		String output = null;
		try {
			/*
			 * create object of InfoquiraService Impl Class 
			 */
			InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
			output = infoquiraServiceImpl.getUserServices().getUsersForLogin(token,userLoginXML);
			// set infoquiraServiceImpl to null
			infoquiraServiceImpl = null;
		} catch (Exception exception) {
			log.error("Get User For Login :: Exception :: >" + exception.getMessage());
			exception.printStackTrace();
		}
		return output;
	}

}
