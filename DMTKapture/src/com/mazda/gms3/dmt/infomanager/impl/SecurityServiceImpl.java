/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import java.rmi.RemoteException;



/**
 * @author darora
 * 
 */
public class SecurityServiceImpl {

	public String createToken(String input) throws RemoteException, Exception 
	{
		// Create Object of InfoquiraServiceImpl class to authenticate the input 
		InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
		return infoquiraServiceImpl.getSecurityServices().authenticate(input);
	}

	public boolean isTokenValid(String token) throws RemoteException, Exception {
		// Create Object of InfoquiraServiceImpl class to validate the token 
		InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
		return infoquiraServiceImpl.getSecurityServices().isTokenValid(token);
	}

}
