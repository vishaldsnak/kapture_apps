package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class AxleTypeDetails {

	private int srNo;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private Long axleTypeId=null;
	private String axleCode=null;
	private String axleCodeDescription=null;
	private String axleCodeDescriptionRegional=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;
	
	private String oldAxleCode=null;
	private String oldAxleCodeDescription=null;
	private String oldAxleCodeDescriptionRegional=null;
	private String oldFlag = null;
	private String syncStatus=null;
	
	
	
	public String getOldAxleCode() {
		return oldAxleCode;
	}
	public void setOldAxleCode(String oldAxleCode) {
		this.oldAxleCode = oldAxleCode;
	}
	public String getOldAxleCodeDescription() {
		return oldAxleCodeDescription;
	}
	public void setOldAxleCodeDescription(String oldAxleCodeDescription) {
		this.oldAxleCodeDescription = oldAxleCodeDescription;
	}
	public String getOldAxleCodeDescriptionRegional() {
		return oldAxleCodeDescriptionRegional;
	}
	public void setOldAxleCodeDescriptionRegional(
			String oldAxleCodeDescriptionRegional) {
		this.oldAxleCodeDescriptionRegional = oldAxleCodeDescriptionRegional;
	}
	public String getOldFlag() {
		return oldFlag;
	}
	public void setOldFlag(String oldFlag) {
		this.oldFlag = oldFlag;
	}
	public String getSyncStatus() {
		return syncStatus;
	}
	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}
	public boolean isShowCheckBox() {
		return showCheckBox;
	}
	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}
	public String getAxleCodeDescriptionRegional() {
		return axleCodeDescriptionRegional;
	}
	public void setAxleCodeDescriptionRegional(String axleCodeDescriptionRegional) {
		this.axleCodeDescriptionRegional = axleCodeDescriptionRegional;
	}
	public Long getCountryLocaleId() {
		return countryLocaleId;
	}
	public void setCountryLocaleId(Long countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
	}
	public Long getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(Long manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
	}
	public Long getAxleTypeId() {
		return axleTypeId;
	}
	public void setAxleTypeId(Long axleTypeId) {
		this.axleTypeId = axleTypeId;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public String getAxleCode() {
		return axleCode;
	}
	public void setAxleCode(String axleCode) {
		this.axleCode = axleCode;
	}
	public String getAxleCodeDescription() {
		return axleCodeDescription;
	}
	public void setAxleCodeDescription(String axleCodeDescription) {
		this.axleCodeDescription = axleCodeDescription;
	}
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	public String getFlagLabel() {
		return flagLabel;
	}
	public void setFlagLabel(String flagLabel) {
		this.flagLabel = flagLabel;
	}
	public Timestamp getEntryTime() {
		return entryTime;
	}
	public void setEntryTime(Timestamp entryTime) {
		this.entryTime = entryTime;
	}
	public Timestamp getUpdatedTime() {
		return updatedTime;
	}
	public void setUpdatedTime(Timestamp updatedTime) {
		this.updatedTime = updatedTime;
	}
	public boolean isEditableFlag() {
		return editableFlag;
	}
	public void setEditableFlag(boolean editableFlag) {
		this.editableFlag = editableFlag;
	}
}