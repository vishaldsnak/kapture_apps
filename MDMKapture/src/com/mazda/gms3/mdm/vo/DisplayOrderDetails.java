package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class DisplayOrderDetails {

	private int srNo;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private Long displayOrderId=null;
	private String modelType=null;
	private String displayOrderCode=null;
	private String displayOrderName=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;

	private boolean saveStatusWhileImport=false;
	
	
	
	public boolean isSaveStatusWhileImport() {
		return saveStatusWhileImport;
	}

	public void setSaveStatusWhileImport(boolean saveStatusWhileImport) {
		this.saveStatusWhileImport = saveStatusWhileImport;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
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

	public Long getDisplayOrderId() {
		return displayOrderId;
	}

	public void setDisplayOrderId(Long displayOrderId) {
		this.displayOrderId = displayOrderId;
	}

	public String getDisplayOrderCode() {
		return displayOrderCode;
	}

	public void setDisplayOrderCode(String displayOrderCode) {
		this.displayOrderCode = displayOrderCode;
	}
	
	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

	public String getDisplayOrderName() {
		return displayOrderName;
	}

	public void setDisplayOrderName(String displayOrderName) {
		this.displayOrderName = displayOrderName;
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

	public boolean isShowCheckBox() {
		return showCheckBox;
	}

	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}
}
