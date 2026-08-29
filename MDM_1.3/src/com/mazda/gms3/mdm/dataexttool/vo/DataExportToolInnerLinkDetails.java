package com.mazda.gms3.mdm.dataexttool.vo;

public class DataExportToolInnerLinkDetails {

	private String innerLinkDocumentId=null;
	private String pathToBeReplaced=null;
	private String srcHrefPath = null;
	private DataExportToolItemDetails itemDetails = null;
	private String processingStatus=null;
	private String errorMessage = null;
	
	private String considerInnerLinkForProcessing=null;
	
	public String getConsiderInnerLinkForProcessing() {
		return considerInnerLinkForProcessing;
	}
	public void setConsiderInnerLinkForProcessing(String considerInnerLinkForProcessing) {
		this.considerInnerLinkForProcessing = considerInnerLinkForProcessing;
	}
	public String getInnerLinkDocumentId() {
		return innerLinkDocumentId;
	}
	public void setInnerLinkDocumentId(String innerLinkDocumentId) {
		this.innerLinkDocumentId = innerLinkDocumentId;
	}
	public String getPathToBeReplaced() {
		return pathToBeReplaced;
	}
	public void setPathToBeReplaced(String pathToBeReplaced) {
		this.pathToBeReplaced = pathToBeReplaced;
	}
	public String getSrcHrefPath() {
		return srcHrefPath;
	}
	public void setSrcHrefPath(String srcHrefPath) {
		this.srcHrefPath = srcHrefPath;
	}
	public DataExportToolItemDetails getItemDetails() {
		return itemDetails;
	}
	public void setItemDetails(DataExportToolItemDetails itemDetails) {
		this.itemDetails = itemDetails;
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
