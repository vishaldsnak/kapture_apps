package com.mazda.gms3.sst.vo;

import java.sql.Timestamp;

public class SSTSectionParentMappingDetails {

	private int srNo;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private Long sstSectionParentMappingId=null;
	private Long sectionId=null;
	private String sectionCode=null;
	private String sectionName=null;
	private Long sstId=null;
	private Long parentSSTId=null;
	private String sstNumber=null;
	private String parentSSTNumber =null;
	private String remarks=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;

	
	
	public String getSectionCode() {
		return sectionCode;
	}

	public void setSectionCode(String sectionCode) {
		this.sectionCode = sectionCode;
	}

	public Long getSstSectionParentMappingId() {
		return sstSectionParentMappingId;
	}

	public void setSstSectionParentMappingId(Long sstSectionParentMappingId) {
		this.sstSectionParentMappingId = sstSectionParentMappingId;
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

	public Long getSectionId() {
		return sectionId;
	}

	public void setSectionId(Long sectionId) {
		this.sectionId = sectionId;
	}

	public String getSectionName() {
		return sectionName;
	}

	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	public Long getSstId() {
		return sstId;
	}

	public void setSstId(Long sstId) {
		this.sstId = sstId;
	}

	public Long getParentSSTId() {
		return parentSSTId;
	}

	public void setParentSSTId(Long parentSSTId) {
		this.parentSSTId = parentSSTId;
	}

	public String getSstNumber() {
		return sstNumber;
	}

	public void setSstNumber(String sstNumber) {
		this.sstNumber = sstNumber;
	}

	public String getParentSSTNumber() {
		return parentSSTNumber;
	}

	public void setParentSSTNumber(String parentSSTNumber) {
		this.parentSSTNumber = parentSSTNumber;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
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