package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class VinCrossReferenceDetails {

	private int srNo;
	private Long vinCrossReferenceId=null;
	private String vinCrossReferenceYear=null;
	private String vinCrossReferenceCode=null;
	private Long countryLocaleId=null;
	private String countryLocaleCode=null;
	private Long manualLanguageId=null;
	private String manualLanguageCode=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	
	private boolean showCheckBox = true;
	
	public Long getCountryLocaleId() {
		return countryLocaleId;
	}

	public void setCountryLocaleId(Long countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
	}

	public String getCountryLocaleCode() {
		return countryLocaleCode;
	}

	public void setCountryLocaleCode(String countryLocaleCode) {
		this.countryLocaleCode = countryLocaleCode;
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

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public Long getVinCrossReferenceId() {
		return vinCrossReferenceId;
	}

	public void setVinCrossReferenceId(Long vinCrossReferenceId) {
		this.vinCrossReferenceId = vinCrossReferenceId;
	}

	public String getVinCrossReferenceYear() {
		return vinCrossReferenceYear;
	}

	public void setVinCrossReferenceYear(String vinCrossReferenceYear) {
		this.vinCrossReferenceYear = vinCrossReferenceYear;
	}

	public String getVinCrossReferenceCode() {
		return vinCrossReferenceCode;
	}

	public void setVinCrossReferenceCode(String vinCrossReferenceCode) {
		this.vinCrossReferenceCode = vinCrossReferenceCode;
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
