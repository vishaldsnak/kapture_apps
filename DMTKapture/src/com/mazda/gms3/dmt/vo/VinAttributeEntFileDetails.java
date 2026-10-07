package com.mazda.gms3.dmt.vo;

public class VinAttributeEntFileDetails {

	private String fileName=null;
	
	private String vinTransType=null;
	private String vinEngineType=null;
	private String vinBodyType=null;
	private String vinBodyTypeValue=null;
	private String vinAxleType=null;
	private String vinAxleTypeValue=null;
	
	
	private String processingStatus=null;
	private String errorMessage=null;
	private String filePath=null;
	
	
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
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
	public String getVinTransType() {
		return vinTransType;
	}
	public void setVinTransType(String vinTransType) {
		this.vinTransType = vinTransType;
	}
	public String getVinEngineType() {
		return vinEngineType;
	}
	public void setVinEngineType(String vinEngineType) {
		this.vinEngineType = vinEngineType;
	}
	public String getVinBodyType() {
		return vinBodyType;
	}
	public void setVinBodyType(String vinBodyType) {
		this.vinBodyType = vinBodyType;
	}
	public String getVinBodyTypeValue() {
		return vinBodyTypeValue;
	}
	public void setVinBodyTypeValue(String vinBodyTypeValue) {
		this.vinBodyTypeValue = vinBodyTypeValue;
	}
	public String getVinAxleType() {
		return vinAxleType;
	}
	public void setVinAxleType(String vinAxleType) {
		this.vinAxleType = vinAxleType;
	}
	public String getVinAxleTypeValue() {
		return vinAxleTypeValue;
	}
	public void setVinAxleTypeValue(String vinAxleTypeValue) {
		this.vinAxleTypeValue = vinAxleTypeValue;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
