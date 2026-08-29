package com.mazda.gms3.mdm.sidataload.vo;

public class SIChannelImageDetails {
	
	private String imageName=null;
	private String imageSourcePath=null;
	private String destinationPath=null;
	private String processingStatus=null;
	private String errorCode=null;
	private String errorMessage=null;
	
	private String srcValue=null;
	private String destinationRelativePath=null;
	private String destinationImageName=null;
	
	
	public String getDestinationImageName() {
		return destinationImageName;
	}
	public void setDestinationImageName(String destinationImageName) {
		this.destinationImageName = destinationImageName;
	}
	public String getSrcValue() {
		return srcValue;
	}
	public void setSrcValue(String srcValue) {
		this.srcValue = srcValue;
	}
	public String getDestinationRelativePath() {
		return destinationRelativePath;
	}
	public void setDestinationRelativePath(String destinationRelativePath) {
		this.destinationRelativePath = destinationRelativePath;
	}
	public String getImageName() {
		return imageName;
	}
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}
	public String getImageSourcePath() {
		return imageSourcePath;
	}
	public void setImageSourcePath(String imageSourcePath) {
		this.imageSourcePath = imageSourcePath;
	}
	public String getDestinationPath() {
		return destinationPath;
	}
	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
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
	
	

}
