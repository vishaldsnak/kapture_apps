package com.mazda.gms3.cdrom.bean;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;

public class CDRomSettingsUtil {

	static Logger logger = LogManager.getLogger(CDRomUtil.class);
	
	public static void saveNetworkPath(String wslId, String networkPath){
		boolean user = isUserAlreadyExist(wslId);
		if(user){
			updatePath(wslId,networkPath);
		}
		else{
			savePath(wslId,networkPath);
		}
	}
	
	private static boolean isUserAlreadyExist(String wslId) {
		// TODO Auto-generated method stub
		return false;
	}

	public static void updatePath(String wslId, String networkPath){
		
	}
	
	public static void savePath(String wslId, String networkPath){
		
	} 
}
