package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class CVCCategoryDetails {



	private int srNo;
	private Long categoryId=null;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private String manualLanguageCode=null;
	private String categoryCode=null;
	private String categoryNameEng=null;
	private String subCategoryCode=null;
	private String subCategoryName=null;
	private String symptomCode=null;
	private String symptomName=null;
	private String subSymptomCode=null;
	private String subSymptomName=null;
	private String conditionCode=null;
	private String conditionName=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private boolean showCheckBox = true;
	private String rank=null;
	
	private String oldCategoryCode=null;
	private String oldCategoryNameEng=null;
	private String oldSubCategoryCode=null;
	private String oldSubCategoryName=null;
	private String oldSymptomCode=null;
	private String oldSymptomName=null;
	private String oldSubSymptomCode=null;
	private String oldSubSymptomName=null;
	private String oldConditionCode=null;
	private String oldConditionName=null;
	private String oldFlag=null;
	private String syncStatus=null;
	
	
	
	public String getOldCategoryCode() {
		return oldCategoryCode;
	}
	public void setOldCategoryCode(String oldCategoryCode) {
		this.oldCategoryCode = oldCategoryCode;
	}
	public String getOldCategoryNameEng() {
		return oldCategoryNameEng;
	}
	public void setOldCategoryNameEng(String oldCategoryNameEng) {
		this.oldCategoryNameEng = oldCategoryNameEng;
	}
	public String getOldSubCategoryCode() {
		return oldSubCategoryCode;
	}
	public void setOldSubCategoryCode(String oldSubCategoryCode) {
		this.oldSubCategoryCode = oldSubCategoryCode;
	}
	public String getOldSubCategoryName() {
		return oldSubCategoryName;
	}
	public void setOldSubCategoryName(String oldSubCategoryName) {
		this.oldSubCategoryName = oldSubCategoryName;
	}
	public String getOldSymptomCode() {
		return oldSymptomCode;
	}
	public void setOldSymptomCode(String oldSymptomCode) {
		this.oldSymptomCode = oldSymptomCode;
	}
	public String getOldSymptomName() {
		return oldSymptomName;
	}
	public void setOldSymptomName(String oldSymptomName) {
		this.oldSymptomName = oldSymptomName;
	}
	public String getOldSubSymptomCode() {
		return oldSubSymptomCode;
	}
	public void setOldSubSymptomCode(String oldSubSymptomCode) {
		this.oldSubSymptomCode = oldSubSymptomCode;
	}
	public String getOldSubSymptomName() {
		return oldSubSymptomName;
	}
	public void setOldSubSymptomName(String oldSubSymptomName) {
		this.oldSubSymptomName = oldSubSymptomName;
	}
	public String getOldConditionCode() {
		return oldConditionCode;
	}
	public void setOldConditionCode(String oldConditionCode) {
		this.oldConditionCode = oldConditionCode;
	}
	public String getOldConditionName() {
		return oldConditionName;
	}
	public void setOldConditionName(String oldConditionName) {
		this.oldConditionName = oldConditionName;
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
	public String getRank() {
		return rank;
	}
	public void setRank(String rank) {
		this.rank = rank;
	}
	public String getSubCategoryCode() {
		return subCategoryCode;
	}
	public void setSubCategoryCode(String subCategoryCode) {
		this.subCategoryCode = subCategoryCode;
	}
	public String getSubCategoryName() {
		return subCategoryName;
	}
	public void setSubCategoryName(String subCategoryName) {
		this.subCategoryName = subCategoryName;
	}
	public String getSymptomCode() {
		return symptomCode;
	}
	public void setSymptomCode(String symptomCode) {
		this.symptomCode = symptomCode;
	}
	public String getSymptomName() {
		return symptomName;
	}
	public void setSymptomName(String symptomName) {
		this.symptomName = symptomName;
	}
	public String getSubSymptomCode() {
		return subSymptomCode;
	}
	public void setSubSymptomCode(String subSymptomCode) {
		this.subSymptomCode = subSymptomCode;
	}
	public String getSubSymptomName() {
		return subSymptomName;
	}
	public void setSubSymptomName(String subSymptomName) {
		this.subSymptomName = subSymptomName;
	}
	public String getConditionCode() {
		return conditionCode;
	}
	public void setConditionCode(String conditionCode) {
		this.conditionCode = conditionCode;
	}
	public String getConditionName() {
		return conditionName;
	}
	public void setConditionName(String conditionName) {
		this.conditionName = conditionName;
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
