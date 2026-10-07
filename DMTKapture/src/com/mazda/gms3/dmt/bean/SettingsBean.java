package com.mazda.gms3.dmt.bean;

import com.mazda.gms3.dmt.vo.SettingDetails;

public class SettingsBean {

	private String errorMessage=null;
	private String successMessage=null;
	
	private SettingDetails fieldDetails = null;
	
	private boolean showSave=false;
	private boolean showUpdate=false;
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getSuccessMessage() {
		return successMessage;
	}
	public void setSuccessMessage(String successMessage) {
		this.successMessage = successMessage;
	}
	public SettingDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(SettingDetails fieldDetails) {
		this.fieldDetails = fieldDetails;
	}
	public boolean isShowSave() {
		return showSave;
	}
	public void setShowSave(boolean showSave) {
		this.showSave = showSave;
	}
	public boolean isShowUpdate() {
		return showUpdate;
	}
	public void setShowUpdate(boolean showUpdate) {
		this.showUpdate = showUpdate;
	}
	
	
}
