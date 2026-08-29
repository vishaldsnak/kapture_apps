package com.mazda.gms3.cdrom.bean;

public class InnerLinkReportBean {
	String manualType;
	String documentId;
	String sourceDocumentId;
	String mappedDocumentHTMLFileName;
	String mappedDocumentHTMLFilePath;
	String mappingStatus;
	
	
	String sourceTag=null;
	String contentToBeReplaced=null;
	
	

	public String getSourceTag() {
		return sourceTag;
	}
	public void setSourceTag(String sourceTag) {
		this.sourceTag = sourceTag;
	}
	public String getContentToBeReplaced() {
		return contentToBeReplaced;
	}
	public void setContentToBeReplaced(String contentToBeReplaced) {
		this.contentToBeReplaced = contentToBeReplaced;
	}
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
	public String getSourceDocumentId() {
		return sourceDocumentId;
	}
	public void setSourceDocumentId(String sourceDocumentId) {
		this.sourceDocumentId = sourceDocumentId;
	}
	public String getMappedDocumentHTMLFileName() {
		return mappedDocumentHTMLFileName;
	}
	public void setMappedDocumentHTMLFileName(String mappedDocumentHTMLFileName) {
		this.mappedDocumentHTMLFileName = mappedDocumentHTMLFileName;
	}
	public String getMappedDocumentHTMLFilePath() {
		return mappedDocumentHTMLFilePath;
	}
	public void setMappedDocumentHTMLFilePath(String mappedDocumentHTMLFilePath) {
		this.mappedDocumentHTMLFilePath = mappedDocumentHTMLFilePath;
	}
	public String getMappingStatus() {
		return mappingStatus;
	}
	public void setMappingStatus(String mappingStatus) {
		this.mappingStatus = mappingStatus;
	}
	
}
