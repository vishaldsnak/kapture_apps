/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import org.apache.log4j.Logger;

/**
 * @author darora
 * 
 */
public class ChannelServiceImpl {

	static Logger log  =Logger.getLogger(ChannelServiceImpl.class);
	
	public String getChannel(String token, String channelName, String localeCode) {
		String channel = null;
		try {
			InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
			channel = infoquiraServiceImpl.getChannelServices().getChannel(token,channelName, localeCode);
			// set infoquiraServiceImpl to null
			infoquiraServiceImpl = null;
		} catch (Exception exception) {
			log.error("Get Channel :: Exception :: >" + exception.getMessage());
			exception.printStackTrace();
		}
		return channel;
	}

}
