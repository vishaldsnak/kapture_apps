package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class MasterDataTypeDetails {

	private int srNo;
	private Long masterDataTypeId=null;
	// C OR V
	private String masterDataType=null;
	// CATEGORY OR VIN ATTRIBUTE
	private String masterDataTypeLabel=null;
	// CVC / ESI / BODY TYPE
	private String masterCategoryName=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private boolean showCheckBox = true;
	
	public boolean isShowCheckBox() {
		return showCheckBox;
	}
	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
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
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getMasterDataTypeId() {
		return masterDataTypeId;
	}
	public void setMasterDataTypeId(Long masterDataTypeId) {
		this.masterDataTypeId = masterDataTypeId;
	}
	public String getMasterDataType() {
		return masterDataType;
	}
	public void setMasterDataType(String masterDataType) {
		this.masterDataType = masterDataType;
	}
	public String getMasterDataTypeLabel() {
		return masterDataTypeLabel;
	}
	public void setMasterDataTypeLabel(String masterDataTypeLabel) {
		this.masterDataTypeLabel = masterDataTypeLabel;
	}
	public String getMasterCategoryName() {
		return masterCategoryName;
	}
	public void setMasterCategoryName(String masterCategoryName) {
		this.masterCategoryName = masterCategoryName;
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