package com.mazda.gms3.sst.vo;

import java.sql.Timestamp;

public class OEMMappingDetails {
	
	private int srNo;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private Long oemMappingId=null;
	private Long sstId=null;
	private String sstNumber=null;
	private String fordNumber=  null;
	private String nissanNumber =null;
	private String suzukiNumber = null;
	private String isuzuNumber = null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;

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

	public Long getOemMappingId() {
		return oemMappingId;
	}

	public void setOemMappingId(Long oemMappingId) {
		this.oemMappingId = oemMappingId;
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

	public String getFordNumber() {
		return fordNumber;
	}

	public void setFordNumber(String fordNumber) {
		this.fordNumber = fordNumber;
	}

	public String getNissanNumber() {
		return nissanNumber;
	}

	public void setNissanNumber(String nissanNumber) {
		this.nissanNumber = nissanNumber;
	}

	public String getSuzukiNumber() {
		return suzukiNumber;
	}

	public void setSuzukiNumber(String suzukiNumber) {
		this.suzukiNumber = suzukiNumber;
	}

	public String getIsuzuNumber() {
		return isuzuNumber;
	}

	public void setIsuzuNumber(String isuzuNumber) {
		this.isuzuNumber = isuzuNumber;
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
