package com.mazda.gms3.dmt.vo;

import java.sql.Timestamp;

public class ManualTypeDetails {

	private Long manualTypeId=null;
	private String manualTypeCode=null;
	private String manualTypeName=null;
	private String manualTypeRefKey=null;
	private Timestamp creationTime=null;
	private Timestamp updatedTime=null;
	private String guid=null;
	
	
	public String getGuid() {
		return guid;
	}
	public void setGuid(String guid) {
		this.guid = guid;
	}
	public Long getManualTypeId() {
		return manualTypeId;
	}
	public void setManualTypeId(Long manualTypeId) {
		this.manualTypeId = manualTypeId;
	}
	public String getManualTypeCode() {
		return manualTypeCode;
	}
	public void setManualTypeCode(String manualTypeCode) {
		this.manualTypeCode = manualTypeCode;
	}
	public String getManualTypeName() {
		return manualTypeName;
	}
	public void setManualTypeName(String manualTypeName) {
		this.manualTypeName = manualTypeName;
	}
	public String getManualTypeRefKey() {
		return manualTypeRefKey;
	}
	public void setManualTypeRefKey(String manualTypeRefKey) {
		this.manualTypeRefKey = manualTypeRefKey;
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
