package com.mazda.gms3.mdm.dataexttool.vo;

public class DataExportToolAttachmentDetails {

	private String name=null;
	private String sourcePath = null;
	private String destinationPath = null;
	private DataExportToolItemDetails itemDetails = null;
	private String processingStatus=null;
	private String errorMessage = null;
	private String attachmentType=null; // ATTACHMENT / IMAGES
	
	// Used for Inline Images & PDFs
	private String pathToBeReplaced=null;
	private String srcHrefPath = null;
	
	
	public String getSrcHrefPath() {
		return srcHrefPath;
	}
	public void setSrcHrefPath(String srcHrefPath) {
		this.srcHrefPath = srcHrefPath;
	}
	public String getPathToBeReplaced() {
		return pathToBeReplaced;
	}
	public void setPathToBeReplaced(String pathToBeReplaced) {
		this.pathToBeReplaced = pathToBeReplaced;
	}
	public String getAttachmentType() {
		return attachmentType;
	}
	public void setAttachmentType(String attachmentType) {
		this.attachmentType = attachmentType;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSourcePath() {
		return sourcePath;
	}
	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}
	public String getDestinationPath() {
		return destinationPath;
	}
	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
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
