package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class CategoryDetails {

	private int srNo;
	private Long categoryId=null;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private String manualLanguageCode=null;
	private String categoryCode=null;
	private String categoryNameEng=null;
//	private String categoryNameReg=null;
	
	private String subCategoryCode=null;
	private String subCategoryNameEng=null;
//	private String subCategoryNameReg=null;
	
	private String subSubCategoryCode=null;
	private String subSubCategoryNameEng=null;
//	private String subSubCategoryNameReg=null;
	
	
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;
	
	private String oldCategoryCode=null;
	private String oldCategoryName=null;
	private String oldSubCategoryName=null;
	private String oldSubCategoryCode=null;
	private String oldSubSubCategoryCode=null;
	private String oldSubSubCategoryName=null;
	private String oldFlag=null;
	private String syncStatus=null;
	
	public String getOldCategoryCode() {
		return oldCategoryCode;
	}
	public void setOldCategoryCode(String oldCategoryCode) {
		this.oldCategoryCode = oldCategoryCode;
	}
	public String getOldCategoryName() {
		return oldCategoryName;
	}
	public void setOldCategoryName(String oldCategoryName) {
		this.oldCategoryName = oldCategoryName;
	}
	public String getOldSubCategoryName() {
		return oldSubCategoryName;
	}
	public void setOldSubCategoryName(String oldSubCategoryName) {
		this.oldSubCategoryName = oldSubCategoryName;
	}
	public String getOldSubCategoryCode() {
		return oldSubCategoryCode;
	}
	public void setOldSubCategoryCode(String oldSubCategoryCode) {
		this.oldSubCategoryCode = oldSubCategoryCode;
	}
	public String getOldSubSubCategoryCode() {
		return oldSubSubCategoryCode;
	}
	public void setOldSubSubCategoryCode(String oldSubSubCategoryCode) {
		this.oldSubSubCategoryCode = oldSubSubCategoryCode;
	}
	public String getOldSubSubCategoryName() {
		return oldSubSubCategoryName;
	}
	public void setOldSubSubCategoryName(String oldSubSubCategoryName) {
		this.oldSubSubCategoryName = oldSubSubCategoryName;
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
	public String getSubCategoryCode() {
		return subCategoryCode;
	}
	public void setSubCategoryCode(String subCategoryCode) {
		this.subCategoryCode = subCategoryCode;
	}
	
	public String getSubCategoryNameEng() {
		return subCategoryNameEng;
	}
	public void setSubCategoryNameEng(String subCategoryNameEng) {
		this.subCategoryNameEng = subCategoryNameEng;
	}
	public String getSubSubCategoryCode() {
		return subSubCategoryCode;
	}
	public void setSubSubCategoryCode(String subSubCategoryCode) {
		this.subSubCategoryCode = subSubCategoryCode;
	}
	public String getSubSubCategoryNameEng() {
		return subSubCategoryNameEng;
	}
	public void setSubSubCategoryNameEng(String subSubCategoryNameEng) {
		this.subSubCategoryNameEng = subSubCategoryNameEng;
	}
	public boolean isShowCheckBox() {
		return showCheckBox;
	}
	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}

	
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getCategoryId() {
		return categoryId;
	}
	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
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
	public String getCategoryCode() {
		return categoryCode;
	}
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	public String getCategoryNameEng() {
		return categoryNameEng;
	}
	public void setCategoryNameEng(String categoryNameEng) {
		this.categoryNameEng = categoryNameEng;
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
