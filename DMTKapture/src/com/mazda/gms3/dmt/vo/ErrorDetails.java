package com.mazda.gms3.dmt.vo;

import java.sql.Timestamp;

public class ErrorDetails {

    private String errorCode = null;
    private String errorMessage = null;
    private String operationType = null;

    private ContentDetails contentDetails = null;

    private Timestamp dateTime = null;

    public Timestamp getDateTime() {
	return dateTime;
    }

    public void setDateTime(Timestamp dateTime) {
	this.dateTime = dateTime;
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

    public String getOperationType() {
	return operationType;
    }

    public void setOperationType(String operationType) {
	this.operationType = operationType;
    }

    public ContentDetails getContentDetails() {
	return contentDetails;
    }

    public void setContentDetails(ContentDetails contentDetails) {
	this.contentDetails = contentDetails;
    }
}