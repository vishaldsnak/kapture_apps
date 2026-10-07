package com.mazda.gms3.dmt.mc.vo;


public class ESICategoryDetails {

	private String faceLiftFolderName=null;
	private String materialFolderName=null;
	private String processingFolderName=null;
	private String fileName=null;
	private String title=null;
	private String categoryLevel1Code=null;
	private String categoryLevel2Code=null;
	private String categoryLevel3Code=null;
	private String filePath=null;
	// WILL BE USED FOR PROCESSING HTML FILE.
	private String fileAbsolutePath=null;
	private String lineType=null;
	
	private String esiCategorySourceFileName=null;
	private String esiCategorySourceFilePath=null;
	
	// ADDED FOR MME WD 
	private String steeringTypeInfoText=null;
	
	private String processingStatus=null;
	private String errorMessage=null;
	private String code1RefKey=null;
	private String code2RefKey=null;
	private String code3RefKey=null;
	private String code1ExistsInMDM=null;
	private String code1ExistsInIM=null;
	
	private String code2ExistsInMDM=null;
	private String code2ExistsInIM=null;
	
	private String code3ExistsInMDM=null;
	private String code3ExistsInIM=null;
	
	private String categoryLevel1Name=null;
	private String categoryLevel2Name=null;
	private String categoryLevel3Name=null;
	
	private String failedSteeringTypeInIM=null;
	
	public String getFailedSteeringTypeInIM() {
		return failedSteeringTypeInIM;
	}

	public void setFailedSteeringTypeInIM(String failedSteeringTypeInIM) {
		this.failedSteeringTypeInIM = failedSteeringTypeInIM;
	}

	public String getCategoryLevel1Name() {
		return categoryLevel1Name;
	}

	public void setCategoryLevel1Name(String categoryLevel1Name) {
		this.categoryLevel1Name = categoryLevel1Name;
	}

	public String getCategoryLevel2Name() {
		return categoryLevel2Name;
	}

	public void setCategoryLevel2Name(String categoryLevel2Name) {
		this.categoryLevel2Name = categoryLevel2Name;
	}

	public String getCategoryLevel3Name() {
		return categoryLevel3Name;
	}

	public void setCategoryLevel3Name(String categoryLevel3Name) {
		this.categoryLevel3Name = categoryLevel3Name;
	}

	public String getCode1RefKey() {
		return code1RefKey;
	}

	public void setCode1RefKey(String code1RefKey) {
		this.code1RefKey = code1RefKey;
	}

	public String getCode2RefKey() {
		return code2RefKey;
	}

	public void setCode2RefKey(String code2RefKey) {
		this.code2RefKey = code2RefKey;
	}

	public String getCode3RefKey() {
		return code3RefKey;
	}

	public void setCode3RefKey(String code3RefKey) {
		this.code3RefKey = code3RefKey;
	}

	public String getCode1ExistsInMDM() {
		return code1ExistsInMDM;
	}

	public void setCode1ExistsInMDM(String code1ExistsInMDM) {
		this.code1ExistsInMDM = code1ExistsInMDM;
	}

	public String getCode1ExistsInIM() {
		return code1ExistsInIM;
	}

	public void setCode1ExistsInIM(String code1ExistsInIM) {
		this.code1ExistsInIM = code1ExistsInIM;
	}

	public String getCode2ExistsInMDM() {
		return code2ExistsInMDM;
	}

	public void setCode2ExistsInMDM(String code2ExistsInMDM) {
		this.code2ExistsInMDM = code2ExistsInMDM;
	}

	public String getCode2ExistsInIM() {
		return code2ExistsInIM;
	}

	public void setCode2ExistsInIM(String code2ExistsInIM) {
		this.code2ExistsInIM = code2ExistsInIM;
	}

	public String getCode3ExistsInMDM() {
		return code3ExistsInMDM;
	}

	public void setCode3ExistsInMDM(String code3ExistsInMDM) {
		this.code3ExistsInMDM = code3ExistsInMDM;
	}

	public String getCode3ExistsInIM() {
		return code3ExistsInIM;
	}

	public void setCode3ExistsInIM(String code3ExistsInIM) {
		this.code3ExistsInIM = code3ExistsInIM;
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

	public String getSteeringTypeInfoText() {
		return steeringTypeInfoText;
	}

	public void setSteeringTypeInfoText(String steeringTypeInfoText) {
		this.steeringTypeInfoText = steeringTypeInfoText;
	}

	public String getFileAbsolutePath() {
		return fileAbsolutePath;
	}

	public void setFileAbsolutePath(String fileAbsolutePath) {
		this.fileAbsolutePath = fileAbsolutePath;
	}

	public String getEsiCategorySourceFilePath() {
		return esiCategorySourceFilePath;
	}

	public void setEsiCategorySourceFilePath(String esiCategorySourceFilePath) {
		this.esiCategorySourceFilePath = esiCategorySourceFilePath;
	}

	public String getEsiCategorySourceFileName() {
		return esiCategorySourceFileName;
	}

	public void setEsiCategorySourceFileName(String esiCategorySourceFileName) {
		this.esiCategorySourceFileName = esiCategorySourceFileName;
	}

	public String getLineType() {
		return lineType;
	}

	public void setLineType(String lineType) {
		this.lineType = lineType;
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
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getCategoryLevel1Code() {
		return categoryLevel1Code;
	}
	public void setCategoryLevel1Code(String categoryLevel1Code) {
		this.categoryLevel1Code = categoryLevel1Code;
	}
	public String getCategoryLevel2Code() {
		return categoryLevel2Code;
	}
	public void setCategoryLevel2Code(String categoryLevel2Code) {
		this.categoryLevel2Code = categoryLevel2Code;
	}
	public String getCategoryLevel3Code() {
		return categoryLevel3Code;
	}
	public void setCategoryLevel3Code(String categoryLevel3Code) {
		this.categoryLevel3Code = categoryLevel3Code;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
}