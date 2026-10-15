package com.mazda.gms3.sst.vo;

import java.sql.Timestamp;

public class SectionModelMappingDetails {

	private int srNo;
	private Long sectionModelMappingId=null;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private String manualLanguageCode=null;
	private String modelName=null;
	private String modelCode=null;
	private Long sectionId=null;
	private String sectionName=null;
	private String sectionCode=null;
	private Long sstId=null;
	private String sstNumber=null;
	private String sstImagePreviewPath=null;
	private String useImagePreviewPath=null;
	
	private String remarks=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;
	
	private SSTDetails sstNumberDetails = null;
	
	
	public String getManualLanguageCode() {
		return manualLanguageCode;
	}

	public void setManualLanguageCode(String manualLanguageCode) {
		this.manualLanguageCode = manualLanguageCode;
	}

	public String getSectionCode() {
		return sectionCode;
	}

	public void setSectionCode(String sectionCode) {
		this.sectionCode = sectionCode;
	}

	public SSTDetails getSstNumberDetails() {
		return sstNumberDetails;
	}

	public void setSstNumberDetails(SSTDetails sstNumberDetails) {
		this.sstNumberDetails = sstNumberDetails;
	}

	public Long getSstId() {
		return sstId;
	}

	public void setSstId(Long sstId) {
		this.sstId = sstId;
	}

	public String getSstNumber() {
		return sstNumber;
	}

	public void setSstNumber(String sstNumber) {
		this.sstNumber = sstNumber;
	}

	public String getSstImagePreviewPath() {
		return sstImagePreviewPath;
	}

	public void setSstImagePreviewPath(String sstImagePreviewPath) {
		this.sstImagePreviewPath = sstImagePreviewPath;
	}

	public String getUseImagePreviewPath() {
		return useImagePreviewPath;
	}

	public void setUseImagePreviewPath(String useImagePreviewPath) {
		this.useImagePreviewPath = useImagePreviewPath;
	}

	public String getModelCode() {
		return modelCode;
	}

	public void setModelCode(String modelCode) {
		this.modelCode = modelCode;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public Long getSectionModelMappingId() {
		return sectionModelMappingId;
	}

	public void setSectionModelMappingId(Long sectionModelMappingId) {
		this.sectionModelMappingId = sectionModelMappingId;
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

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
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
