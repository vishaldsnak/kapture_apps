package com.mazda.gms3.cdrom.bean;


public class CDRomSettingsBean {

	private String errorMessage = null;
	private String successMessage = null;

	private CDRomSettingDetails fieldDetails = null;

	private boolean showSave = false;
	private boolean showUpdate = false;
	private boolean showReadControls = false;
	private boolean showWriteControls = false;

	public boolean isShowReadControls() {
		return showReadControls;
	}

	public void setShowReadControls(boolean showReadControls) {
		this.showReadControls = showReadControls;
	}

	public boolean isShowWriteControls() {
		return showWriteControls;
	}

	public void setShowWriteControls(boolean showWriteControls) {
		this.showWriteControls = showWriteControls;
	}

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

	public CDRomSettingDetails getFieldDetails() {
		return fieldDetails;
	}

	public void setFieldDetails(CDRomSettingDetails fieldDetails) {
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
