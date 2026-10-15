package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class CarlineDetails {
	
	private int srNo;
	private Long carlineCodeId=null;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private String manualLanguageCode=null;
	private String carlineCode=null;
	private String yearStart=null;
	private String yearEnd=null;
	private String carlineNameEng=null;
	private String carlineNameReg=null;
	
	private String modelType=null;
	private String esiCategoryFlag=null;
	private String esiCategoryFlagLabel=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private String wmiCode=null;
	
	private boolean showCheckBox = true;
	
	private String countryLocaleCode=null;
	
	// COMBINATION OF ENG NAME + CARLINE CODE
	private String carlineIdForCombo=null;
	
	// REQUIRED FOR CARLINE IM NOTOFICATION BATCH JOB OPERATIONS 
	private String carlineNotificationSent=null;
	private String carlineNotificationYearEndOnly=null;
	private String carlineNotifcationPreYearValue=null;
	private Timestamp carlineNotifcationSentTimestamp=null;
	
	private String previousYearEnd=null;
	
	private boolean SaveStatusWhileImport = false;
	
	private String icDisplayCarlineCode=null;
	
	
	// AUTOMATION VARIABLES
	private String oldCarlineCode=null;
	private String oldCarlineNameEng=null;
	private String oldCarlineNameReg=null;
	private String oldYearStart=null;
	private String oldYearEnd=null;
	private String oldFlag=null;
	private String syncStatus=null;
	
	
	public String getOldFlag() {
		return oldFlag;
	}
	public void setOldFlag(String oldFlag) {
		this.oldFlag = oldFlag;
	}
	public String getOldCarlineCode() {
		return oldCarlineCode;
	}
	public void setOldCarlineCode(String oldCarlineCode) {
		this.oldCarlineCode = oldCarlineCode;
	}
	public String getOldCarlineNameEng() {
		return oldCarlineNameEng;
	}
	public void setOldCarlineNameEng(String oldCarlineNameEng) {
		this.oldCarlineNameEng = oldCarlineNameEng;
	}
	public String getOldCarlineNameReg() {
		return oldCarlineNameReg;
	}
	public void setOldCarlineNameReg(String oldCarlineNameReg) {
		this.oldCarlineNameReg = oldCarlineNameReg;
	}
	public String getOldYearStart() {
		return oldYearStart;
	}
	public void setOldYearStart(String oldYearStart) {
		this.oldYearStart = oldYearStart;
	}
	public String getOldYearEnd() {
		return oldYearEnd;
	}
	public void setOldYearEnd(String oldYearEnd) {
		this.oldYearEnd = oldYearEnd;
	}
	public String getSyncStatus() {
		return syncStatus;
	}
	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}
	public String getIcDisplayCarlineCode() {
		return icDisplayCarlineCode;
	}
	public void setIcDisplayCarlineCode(String icDisplayCarlineCode) {
		this.icDisplayCarlineCode = icDisplayCarlineCode;
	}
	public boolean isSaveStatusWhileImport() {
		return SaveStatusWhileImport;
	}
	public void setSaveStatusWhileImport(boolean saveStatusWhileImport) {
		SaveStatusWhileImport = saveStatusWhileImport;
	}
	public Timestamp getCarlineNotifcationSentTimestamp() {
		return carlineNotifcationSentTimestamp;
	}
	public void setCarlineNotifcationSentTimestamp(
			Timestamp carlineNotifcationSentTimestamp) {
		this.carlineNotifcationSentTimestamp = carlineNotifcationSentTimestamp;
	}
	public String getCarlineNotifcationPreYearValue() {
		return carlineNotifcationPreYearValue;
	}
	public void setCarlineNotifcationPreYearValue(
			String carlineNotifcationPreYearValue) {
		this.carlineNotifcationPreYearValue = carlineNotifcationPreYearValue;
	}
	public String getCarlineNotificationSent() {
		return carlineNotificationSent;
	}
	public void setCarlineNotificationSent(String carlineNotificationSent) {
		this.carlineNotificationSent = carlineNotificationSent;
	}
	public String getCarlineNotificationYearEndOnly() {
		return carlineNotificationYearEndOnly;
	}
	public void setCarlineNotificationYearEndOnly(
			String carlineNotificationYearEndOnly) {
		this.carlineNotificationYearEndOnly = carlineNotificationYearEndOnly;
	}
	public String getPreviousYearEnd() {
		return previousYearEnd;
	}
	public void setPreviousYearEnd(String previousYearEnd) {
		this.previousYearEnd = previousYearEnd;
	}
	
	public String getCarlineIdForCombo() {
		return carlineIdForCombo;
	}
	public void setCarlineIdForCombo(String carlineIdForCombo) {
		this.carlineIdForCombo = carlineIdForCombo;
	}
	public String getCountryLocaleCode() {
		return countryLocaleCode;
	}
	public void setCountryLocaleCode(String countryLocaleCode) {
		this.countryLocaleCode = countryLocaleCode;
	}
	public String getModelType() {
		return modelType;
	}
	public void setModelType(String modelType) {
		this.modelType = modelType;
	}
	public String getEsiCategoryFlag() {
		return esiCategoryFlag;
	}
	public void setEsiCategoryFlag(String esiCategoryFlag) {
		this.esiCategoryFlag = esiCategoryFlag;
	}
	public String getEsiCategoryFlagLabel() {
		return esiCategoryFlagLabel;
	}
	public void setEsiCategoryFlagLabel(String esiCategoryFlagLabel) {
		this.esiCategoryFlagLabel = esiCategoryFlagLabel;
	}
	public Long getCarlineCodeId() {
		return carlineCodeId;
	}
	public void setCarlineCodeId(Long carlineCodeId) {
		this.carlineCodeId = carlineCodeId;
	}
	public boolean isShowCheckBox() {
		return showCheckBox;
	}
	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}

	public String getWmiCode() {
		return wmiCode;
	}
	public void setWmiCode(String wmiCode) {
		this.wmiCode = wmiCode;
	}
	public String getYearStart() {
		return yearStart;
	}
	public void setYearStart(String yearStart) {
		this.yearStart = yearStart;
	}
	public String getYearEnd() {
		return yearEnd;
	}
	public void setYearEnd(String yearEnd) {
		this.yearEnd = yearEnd;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(Long manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
	}
	public String getManualLanguageCode() {
		return manualLanguageCode;
	}
	public void setManualLanguageCode(String manualLanguageCode) {
		this.manualLanguageCode = manualLanguageCode;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getCarlineNameEng() {
		return carlineNameEng;
	}
	public void setCarlineNameEng(String carlineNameEng) {
		this.carlineNameEng = carlineNameEng;
	}
	public String getCarlineNameReg() {
		return carlineNameReg;
	}
	public void setCarlineNameReg(String carlineNameReg) {
		this.carlineNameReg = carlineNameReg;
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
	public Long getCountryLocaleId() {
		return countryLocaleId;
	}
	public void setCountryLocaleId(Long countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
	}

	/*
	 * Import action / marker column read from the LAST column of the import Excel.
	 * Business values: A/ADD/Add (create), U/update/Update (update), D/delete/Delete
	 * (soft delete). Blank or any non-delete value falls through to the existing
	 * create-or-update path, which is left exactly as it was.
	 */
	private String importAction=null;

	public String getImportAction() {
		return importAction;
	}

	public void setImportAction(String importAction) {
		this.importAction = importAction;
	}
}