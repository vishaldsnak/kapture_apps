package com.mazda.gms3.dmt.mc.vo;

import java.util.List;

public class CDProcessingDetails {

	private int srNo;
	
	private String modelFolderName=null;
	private String manualType=null;
	private String manualTypeRefKey=null;
	private String faceLiftFolderName=null;
	private String materialFolderName=null;
	private String processingFolderName=null;
	private String fileName=null;
	private String filePath=null;
	private String title=null;
	
	private String displayOrderLevel1Code=null;
	private String displayOrderLevel2Code=null;
	private String displayOrderLevel3Code=null;
	private String displayOrderLevel4Code=null;
	private String displayOrderLevel5Code=null;
	private String displayOrderLevel6Code=null;
	private String displayOrderLevel1Name=null;
	private String displayOrderLevel2Name=null;
	private String displayOrderLevel3Name=null;
	private String displayOrderLevel4Name=null;
	private String displayOrderLevel5Name=null;
	private String displayOrderLevel6Name=null;

	private String cdProcessingSourceFileName=null;
	private String cdProcessingSourceFilePath=null;
	
	private String sequenceNo=null;
	private String engineType=null;
	private String missionType=null;
	private String driveAxleType=null;
	private String bodyType=null;
	
	private String lineType=null;
	
	// VARIABLES REQUIRED FOR SEPARATE CD PROCESSING ALL TOGETHER.
	private String modelType=null;
	private String documentId=null;
	private String processingStatus=null;
	private String errorCode=null;
	private String errorMessage = null;
	private String locale=null;
	private String scheduleId=null;
	private String itemId=null;
	
	private boolean engineATMT=false;
	
	private List<CDProcessingDetails> childList = null;
	
	

	public List<CDProcessingDetails> getChildList() {
		return childList;
	}

	public void setChildList(List<CDProcessingDetails> childList) {
		this.childList = childList;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public boolean isEngineATMT() {
		return engineATMT;
	}

	public void setEngineATMT(boolean engineATMT) {
		this.engineATMT = engineATMT;
	}
	public String getItemId() {
		return itemId;
	}
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
	public String getModelFolderName() {
		return modelFolderName;
	}
	public void setModelFolderName(String modelFolderName) {
		this.modelFolderName = modelFolderName;
	}
	public String getManualType() {
		return manualType;
	}
	public void setManualType(String manualType) {
		this.manualType = manualType;
	}
	public String getFaceLiftFolderName() {
		return faceLiftFolderName;
	}
	public void setFaceLiftFolderName(String faceLiftFolderName) {
		this.faceLiftFolderName = faceLiftFolderName;
	}
	public String getMaterialFolderName() {
		return materialFolderName;
	}
	public void setMaterialFolderName(String materialFolderName) {
		this.materialFolderName = materialFolderName;
	}
	public String getProcessingFolderName() {
		return processingFolderName;
	}
	public void setProcessingFolderName(String processingFolderName) {
		this.processingFolderName = processingFolderName;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDisplayOrderLevel1Code() {
		return displayOrderLevel1Code;
	}
	public void setDisplayOrderLevel1Code(String displayOrderLevel1Code) {
		this.displayOrderLevel1Code = displayOrderLevel1Code;
	}
	public String getDisplayOrderLevel2Code() {
		return displayOrderLevel2Code;
	}
	public void setDisplayOrderLevel2Code(String displayOrderLevel2Code) {
		this.displayOrderLevel2Code = displayOrderLevel2Code;
	}
	public String getDisplayOrderLevel3Code() {
		return displayOrderLevel3Code;
	}
	public void setDisplayOrderLevel3Code(String displayOrderLevel3Code) {
		this.displayOrderLevel3Code = displayOrderLevel3Code;
	}
	public String getDisplayOrderLevel4Code() {
		return displayOrderLevel4Code;
	}
	public void setDisplayOrderLevel4Code(String displayOrderLevel4Code) {
		this.displayOrderLevel4Code = displayOrderLevel4Code;
	}
	public String getDisplayOrderLevel5Code() {
		return displayOrderLevel5Code;
	}
	public void setDisplayOrderLevel5Code(String displayOrderLevel5Code) {
		this.displayOrderLevel5Code = displayOrderLevel5Code;
	}
	public String getDisplayOrderLevel6Code() {
		return displayOrderLevel6Code;
	}
	public void setDisplayOrderLevel6Code(String displayOrderLevel6Code) {
		this.displayOrderLevel6Code = displayOrderLevel6Code;
	}
	public String getDisplayOrderLevel1Name() {
		return displayOrderLevel1Name;
	}
	public void setDisplayOrderLevel1Name(String displayOrderLevel1Name) {
		this.displayOrderLevel1Name = displayOrderLevel1Name;
	}
	public String getDisplayOrderLevel2Name() {
		return displayOrderLevel2Name;
	}
	public void setDisplayOrderLevel2Name(String displayOrderLevel2Name) {
		this.displayOrderLevel2Name = displayOrderLevel2Name;
	}
	public String getDisplayOrderLevel3Name() {
		return displayOrderLevel3Name;
	}
	public void setDisplayOrderLevel3Name(String displayOrderLevel3Name) {
		this.displayOrderLevel3Name = displayOrderLevel3Name;
	}
	public String getDisplayOrderLevel4Name() {
		return displayOrderLevel4Name;
	}
	public void setDisplayOrderLevel4Name(String displayOrderLevel4Name) {
		this.displayOrderLevel4Name = displayOrderLevel4Name;
	}
	public String getDisplayOrderLevel5Name() {
		return displayOrderLevel5Name;
	}
	public void setDisplayOrderLevel5Name(String displayOrderLevel5Name) {
		this.displayOrderLevel5Name = displayOrderLevel5Name;
	}
	public String getDisplayOrderLevel6Name() {
		return displayOrderLevel6Name;
	}
	public void setDisplayOrderLevel6Name(String displayOrderLevel6Name) {
		this.displayOrderLevel6Name = displayOrderLevel6Name;
	}
	public String getCdProcessingSourceFileName() {
		return cdProcessingSourceFileName;
	}
	public void setCdProcessingSourceFileName(String cdProcessingSourceFileName) {
		this.cdProcessingSourceFileName = cdProcessingSourceFileName;
	}
	public String getCdProcessingSourceFilePath() {
		return cdProcessingSourceFilePath;
	}
	public void setCdProcessingSourceFilePath(String cdProcessingSourceFilePath) {
		this.cdProcessingSourceFilePath = cdProcessingSourceFilePath;
	}
	public String getSequenceNo() {
		return sequenceNo;
	}
	public void setSequenceNo(String sequenceNo) {
		this.sequenceNo = sequenceNo;
	}
	public String getEngineType() {
		return engineType;
	}
	public void setEngineType(String engineType) {
		this.engineType = engineType;
	}
	public String getMissionType() {
		return missionType;
	}
	public void setMissionType(String missionType) {
		this.missionType = missionType;
	}
	public String getDriveAxleType() {
		return driveAxleType;
	}
	public void setDriveAxleType(String driveAxleType) {
		this.driveAxleType = driveAxleType;
	}
	public String getBodyType() {
		return bodyType;
	}
	public void setBodyType(String bodyType) {
		this.bodyType = bodyType;
	}
	public String getLineType() {
		return lineType;
	}
	public void setLineType(String lineType) {
		this.lineType = lineType;
	}
	public String getModelType() {
		return modelType;
	}
	public void setModelType(String modelType) {
		this.modelType = modelType;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
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
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}
	public String getManualTypeRefKey() {
		return manualTypeRefKey;
	}
	public void setManualTypeRefKey(String manualTypeRefKey) {
		this.manualTypeRefKey = manualTypeRefKey;
	}
}