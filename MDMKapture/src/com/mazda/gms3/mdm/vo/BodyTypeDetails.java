package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class BodyTypeDetails {

	private int srNo;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private Long bodyTypeId=null;
	private String bodyCode=null;
	private String bodyCodeDescription=null;
	private String bodyCodeDescriptionRegional=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;
	
	private String oldBodyCode=null;
	private String oldBodyCodeDescription=null;
	private String oldBodyCodeDescriptionRegional=null;
	private String oldFlag=null;
	private String syncStatus=null;
	
	
	
	public String getOldBodyCode() {
		return oldBodyCode;
	}
	public void setOldBodyCode(String oldBodyCode) {
		this.oldBodyCode = oldBodyCode;
	}
	public String getOldBodyCodeDescription() {
		return oldBodyCodeDescription;
	}
	public void setOldBodyCodeDescription(String oldBodyCodeDescription) {
		this.oldBodyCodeDescription = oldBodyCodeDescription;
	}
	public String getOldBodyCodeDescriptionRegional() {
		return oldBodyCodeDescriptionRegional;
	}
	public void setOldBodyCodeDescriptionRegional(
			String oldBodyCodeDescriptionRegional) {
		this.oldBodyCodeDescriptionRegional = oldBodyCodeDescriptionRegional;
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
	public String getBodyCodeDescriptionRegional() {
		return bodyCodeDescriptionRegional;
	}
	public void setBodyCodeDescriptionRegional(String bodyCodeDescriptionRegional) {
		this.bodyCodeDescriptionRegional = bodyCodeDescriptionRegional;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getBodyTypeId() {
		return bodyTypeId;
	}
	public void setBodyTypeId(Long bodyTypeId) {
		this.bodyTypeId = bodyTypeId;
	}
	public String getBodyCode() {
		return bodyCode;
	}
	public void setBodyCode(String bodyCode) {
		this.bodyCode = bodyCode;
	}
	public String getBodyCodeDescription() {
		return bodyCodeDescription;
	}
	public void setBodyCodeDescription(String bodyCodeDescription) {
		this.bodyCodeDescription = bodyCodeDescription;
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

	/** Excel import Action / Indicator column: A / U / D, or blank (= add or update). */
	private String importAction=null;
	public String getImportAction() {
		return importAction;
	}
	public void setImportAction(String importAction) {
		this.importAction = importAction;
	}
}
