package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class CountryLocaleDetails {

	private int srNo;
	private Long countryLocaleId=null;
	private String countryLocaleCode=null;
	private String countryLocaleDesc=null;
	private String flag = null;
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
	public String getCountryLocaleCode() {
		return countryLocaleCode;
	}
	public void setCountryLocaleCode(String countryLocaleCode) {
		this.countryLocaleCode = countryLocaleCode;
	}
	public String getCountryLocaleDesc() {
		return countryLocaleDesc;
	}
	public void setCountryLocaleDesc(String countryLocaleDesc) {
		this.countryLocaleDesc = countryLocaleDesc;
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
}