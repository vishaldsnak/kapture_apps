package com.mazda.gms3.sst.vo;

import java.sql.Timestamp;

public class SSTImageDetails {

	private int srNo;
	private Long sstImageId=null;
	private String sstImageCode=null;
	private String sstImageRevision=null;
	private String sstImagePath=null;
	private String sstImagePreviewPath=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	
	private boolean showCheckBox = true;
	
	

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

	public Long getSstImageId() {
		return sstImageId;
	}

	public void setSstImageId(Long sstImageId) {
		this.sstImageId = sstImageId;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	
	public String getSstImageCode() {
		return sstImageCode;
	}

	public void setSstImageCode(String sstImageCode) {
		this.sstImageCode = sstImageCode;
	}

	public String getSstImageRevision() {
		return sstImageRevision;
	}

	public void setSstImageRevision(String sstImageRevision) {
		this.sstImageRevision = sstImageRevision;
	}

	public String getSstImagePath() {
		return sstImagePath;
	}

	public void setSstImagePath(String sstImagePath) {
		this.sstImagePath = sstImagePath;
	}

	public String getSstImagePreviewPath() {
		return sstImagePreviewPath;
	}

	public void setSstImagePreviewPath(String sstImagePreviewPath) {
		this.sstImagePreviewPath = sstImagePreviewPath;
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
