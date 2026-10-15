package com.mazda.gms3.mdm.vo;

public class ContentDetails {

	private String documentId=null;
	private String locale=null;
	private String fetchedVersion=null;
	private String xml = null;
	private String errorCode=null;
	private String errorMessage=null;
	private String processingStatus=null;
	
	
	private String fileName=null;
	private String modifiedVersion=null;
	private String contentId=null;
	private String publishStatus=null;
	
	/*
	 * THE KAPTURE READ THIS UPDATE IS BASED ON - replaces the InQuira ContentRecordITO.
	 *
	 * HELD RATHER THAN RE-READ, because the write endpoints rewrite the whole row: the payload has
	 * to hand back every field of the version that was merged, and a second read could return a
	 * different one. Typed as Object so this VO stays free of the Kapture classes, as it was free
	 * of the InQuira ones in everything but this field.
	 */
	private Object readResult = null;
	
	private boolean titleUpdateStatus=false;
	
	
	
	public boolean isTitleUpdateStatus() {
		return titleUpdateStatus;
	}
	public void setTitleUpdateStatus(boolean titleUpdateStatus) {
		this.titleUpdateStatus = titleUpdateStatus;
	}
	public Object getReadResult() {
		return readResult;
	}
	public void setReadResult(Object readResult) {
		this.readResult = readResult;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getModifiedVersion() {
		return modifiedVersion;
	}
	public void setModifiedVersion(String modifiedVersion) {
		this.modifiedVersion = modifiedVersion;
	}
	public String getContentId() {
		return contentId;
	}
	public void setContentId(String contentId) {
		this.contentId = contentId;
	}
	public String getPublishStatus() {
		return publishStatus;
	}
	public void setPublishStatus(String publishStatus) {
		this.publishStatus = publishStatus;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getFetchedVersion() {
		return fetchedVersion;
	}
	public void setFetchedVersion(String fetchedVersion) {
		this.fetchedVersion = fetchedVersion;
	}
	public String getXml() {
		return xml;
	}
	public void setXml(String xml) {
		this.xml = xml;
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
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
}