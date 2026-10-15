package com.mazda.gms3.cdrom.bean;

public class DocumentFailureReportBean {
	String manualType;
	public String getManualType() {
		return manualType;
	}
	public void setManualType(String manualType) {
		this.manualType = manualType;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getErrorTime() {
		return errorTime;
	}
	public void setErrorTime(String errorTime) {
		this.errorTime = errorTime;
	}
	String documentId;
	String errorCode;
	String errorMessage;
	String errorTime;
}
