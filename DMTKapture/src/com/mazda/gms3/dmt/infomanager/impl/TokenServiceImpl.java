/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import org.apache.log4j.Logger;


/**
 * @author darora
 * 
 */
public class TokenServiceImpl{
	
	static Logger log = Logger.getLogger(TokenServiceImpl.class);
	

	public String getToken(String input) {

		String token = null;
		try {
			SecurityServiceImpl securityServiceImpl = new SecurityServiceImpl();
			token = securityServiceImpl.createToken(input);
			// set securityServiceImpl t0 null
			securityServiceImpl = null;
			/*
			 * List<TokenEntity> tokenEntities =
			 * getTokenDAO().getTokenEntities(); if(tokenEntities.isEmpty()){
			 * token = getSecurityService().createToken(input); TokenEntity
			 * tokenEntity = new TokenEntity(); tokenEntity.setToken(token);
			 * getTokenDAO().saveOrUpdateToken(tokenEntity); } else{ TokenEntity
			 * tokenEntity = tokenEntities.get(0); boolean tokenValid =
			 * getSecurityService().isTokenValid(tokenEntity.getToken());
			 * if(!tokenValid){ token = getSecurityService().createToken(input);
			 * tokenEntity.setToken(token);
			 * getTokenDAO().saveOrUpdateToken(tokenEntity); }else{ token =
			 * tokenEntity.getToken(); } }
			 */
		} catch (Exception e) {
			log.error("Get Token :: Exception :: >" + e.getMessage());
			e.printStackTrace();
		}

		return token;
	}

	public boolean isTokenValid(String token) {
		boolean valid = false;
		try {
			SecurityServiceImpl securityServiceImpl = new SecurityServiceImpl();
			valid = securityServiceImpl.isTokenValid(token);
			// set securityServiceImpl to null;
			securityServiceImpl = null;
		} catch (Exception exception) {
			log.error("Is Token Valid :: Exception :: >" + exception.getMessage());
			exception.printStackTrace();
		}
		return valid;
	}
}