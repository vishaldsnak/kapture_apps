package com.mazda.gms3.sst.vo;

import java.sql.Timestamp;

public class SSTMDMVinDetails {

	private int srNo;
	private Long sstMdmVinMappingId=null;
	private Long countryLocaleId=null;
	private Long manualLanguageId=null;
	private String carlineComboId=null;
	private String carlineCode=null;
	private String carlineNameEng=null;
	private String carlineNameReg=null;
	private String wmiCode=null;
	private String vdsCode=null;
	private String visStart=null;
	private String visEnd=  null;
	private String sstCarlineCode=null;
	
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;

	public Long getSstMdmVinMappingId() {
		return sstMdmVinMappingId;
	}

	public void setSstMdmVinMappingId(Long sstMdmVinMappingId) {
		this.sstMdmVinMappingId = sstMdmVinMappingId;
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

	public String getWmiCode() {
		return wmiCode;
	}

	public void setWmiCode(String wmiCode) {
		this.wmiCode = wmiCode;
	}

	public String getVdsCode() {
		return vdsCode;
	}

	public void setVdsCode(String vdsCode) {
		this.vdsCode = vdsCode;
	}

	public String getVisStart() {
		return visStart;
	}

	public void setVisStart(String visStart) {
		this.visStart = visStart;
	}

	public String getVisEnd() {
		return visEnd;
	}

	public void setVisEnd(String visEnd) {
		this.visEnd = visEnd;
	}

	public String getSstCarlineCode() {
		return sstCarlineCode;
	}

	public void setSstCarlineCode(String sstCarlineCode) {
		this.sstCarlineCode = sstCarlineCode;
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

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public String getCarlineComboId() {
		return carlineComboId;
	}

	public void setCarlineComboId(String carlineComboId) {
		this.carlineComboId = carlineComboId;
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
