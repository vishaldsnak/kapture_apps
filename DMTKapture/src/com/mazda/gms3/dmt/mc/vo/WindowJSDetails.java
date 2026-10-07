package com.mazda.gms3.dmt.mc.vo;

import com.mazda.gms3.dmt.vo.ContentDetails;

public class WindowJSDetails {

	private String filePath=null;
	private String fileName=null;
	private ContentDetails contentDetails=null;
	private String innerLinkPath=null;
	private String innerLinkDocumentId=null;
	private String processingStatus=null;
	private String errorMessage=null;
	private String preapredInnerLinkURLForIC=null;
	
	
	private String aTagString=null;
	
	public String getaTagString() {
		return aTagString;
	}
	public void setaTagString(String aTagString) {
		this.aTagString = aTagString;
	}
	public String getPreapredInnerLinkURLForIC() {
		return preapredInnerLinkURLForIC;
	}
	public void setPreapredInnerLinkURLForIC(String preapredInnerLinkURLForIC) {
		this.preapredInnerLinkURLForIC = preapredInnerLinkURLForIC;
	}
	public ContentDetails getContentDetails() {
		return contentDetails;
	}
	public void setContentDetails(ContentDetails contentDetails) {
		this.contentDetails = contentDetails;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getInnerLinkPath() {
		return innerLinkPath;
	}
	public void setInnerLinkPath(String innerLinkPath) {
		this.innerLinkPath = innerLinkPath;
	}
	public String getInnerLinkDocumentId() {
		return innerLinkDocumentId;
	}
	public void setInnerLinkDocumentId(String innerLinkDocumentId) {
		this.innerLinkDocumentId = innerLinkDocumentId;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
