package com.mazda.gms3.cdrom.bean;

import java.sql.Timestamp;

public class CDRomSettingDetails {
	
	private String userId=null;
	private String networkPath=null;
	private Timestamp creationTime=null;
	private Timestamp updatedTime=null;
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getNetworkPath() {
		return networkPath;
	}
	public void setNetworkPath(String networkPath) {
		this.networkPath = networkPath;
	}
	public Timestamp getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Timestamp creationTime) {
		this.creationTime = creationTime;
	}
	public Timestamp getUpdatedTime() {
		return updatedTime;
	}
	public void setUpdatedTime(Timestamp updatedTime) {
		this.updatedTime = updatedTime;
	}
}
