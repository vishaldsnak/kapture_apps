package com.mazda.gms3.dmt.mc.vo;

public class VinDetails {

	private String manualType=null;
	private String faceLiftFolder=null;
	private String materialFolder=null;
	
	
	private String carlineCode=null;
	private String vdsCode=null;
	private String visStartRange=null;
	private String visEndRange=null;
	
	private String displayOrderTextFileName=null;
	private String vinSourceFileName=null;
	private String vinSorceFilePath=null;
	
	private String lineType=null;
	private String vinRefKey=null;
	private String wmiCode=null;
	
	private String vinFoundInMDM=null;
	private String vinFoundInIM=null;
	private String errorMessage=null;
	private String processingStatus=null;
	
	private String carlineNameEng=null;
	private String carlineNameReg=null;
	
	
	private boolean allTokensExists = false;
	
	/*
	 * BELOW VARIABLES WILL BE USED BY SCM VIN TXT FILE
	 */
	private String docSourceFileName=null;
	private String docSourceFilePath=null;
	private String docTitle=null;
	private String documentId=null;
	
	private String modelType=null;
	private String locale=null;
	private String scheduleId=null;
	private String itemId=null;
	
	private String errorCode=null;
	private String processingFolder=null;

	/*
	 * MNAO vin.txt ONLY: the MODEL and the MODEL YEAR given after the VIS end range
	 * (...||<MODEL>||<YEAR>MY||<DISPLAY ORDER FILE>) - the model year category is made from them.
	 * modelYear is kept as given (e.g. 2026MY); the category takes off the MY.
	 */
	private String modelName=null;
	private String modelYear=null;

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getModelYear() {
		return modelYear;
	}

	public void setModelYear(String modelYear) {
		this.modelYear = modelYear;
	}
	
	public String getProcessingFolder() {
		return processingFolder;
	}

	public void setProcessingFolder(String processingFolder) {
		this.processingFolder = processingFolder;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
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

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getDocSourceFileName() {
		return docSourceFileName;
	}

	public void setDocSourceFileName(String docSourceFileName) {
		this.docSourceFileName = docSourceFileName;
	}

	public String getDocSourceFilePath() {
		return docSourceFilePath;
	}

	public void setDocSourceFilePath(String docSourceFilePath) {
		this.docSourceFilePath = docSourceFilePath;
	}

	public String getDocTitle() {
		return docTitle;
	}

	public void setDocTitle(String docTitle) {
		this.docTitle = docTitle;
	}

	public boolean isAllTokensExists() {
		return allTokensExists;
	}

	public void setAllTokensExists(boolean allTokensExists) {
		this.allTokensExists = allTokensExists;
	}

	public String getCarlineNameEng() {
		return carlineNameEng;
	}

	public void setCarlineNameEng(String carlineNameEng) {
		this.carlineNameEng = carlineNameEng;
	}

	public String getCarlineNameReg() {
		return carlineNameReg;
	}

	public void setCarlineNameReg(String carlineNameReg) {
		this.carlineNameReg = carlineNameReg;
	}

	public String getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}

	public String getVinFoundInMDM() {
		return vinFoundInMDM;
	}

	public void setVinFoundInMDM(String vinFoundInMDM) {
		this.vinFoundInMDM = vinFoundInMDM;
	}

	public String getVinFoundInIM() {
		return vinFoundInIM;
	}

	public void setVinFoundInIM(String vinFoundInIM) {
		this.vinFoundInIM = vinFoundInIM;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getWmiCode() {
		return wmiCode;
	}

	public void setWmiCode(String wmiCode) {
		this.wmiCode = wmiCode;
	}

	public String getVinRefKey() {
		return vinRefKey;
	}

	public void setVinRefKey(String vinRefKey) {
		this.vinRefKey = vinRefKey;
	}

	public String getVinSorceFilePath() {
		return vinSorceFilePath;
	}

	public void setVinSorceFilePath(String vinSorceFilePath) {
		this.vinSorceFilePath = vinSorceFilePath;
	}

	public String getLineType() {
		return lineType;
	}

	public void setLineType(String lineType) {
		this.lineType = lineType;
	}

	public String getVinSourceFileName() {
		return vinSourceFileName;
	}

	public void setVinSourceFileName(String vinSourceFileName) {
		this.vinSourceFileName = vinSourceFileName;
	}

	public String getManualType() {
		return manualType;
	}

	public void setManualType(String manualType) {
		this.manualType = manualType;
	}

	public String getFaceLiftFolder() {
		return faceLiftFolder;
	}

	public void setFaceLiftFolder(String faceLiftFolder) {
		this.faceLiftFolder = faceLiftFolder;
	}

	public String getMaterialFolder() {
		return materialFolder;
	}

	public void setMaterialFolder(String materialFolder) {
		this.materialFolder = materialFolder;
	}

	public String getCarlineCode() {
		return carlineCode;
	}

	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}

	public String getVdsCode() {
		return vdsCode;
	}

	public void setVdsCode(String vdsCode) {
		this.vdsCode = vdsCode;
	}

	public String getVisStartRange() {
		return visStartRange;
	}

	public void setVisStartRange(String visStartRange) {
		this.visStartRange = visStartRange;
	}

	public String getVisEndRange() {
		return visEndRange;
	}

	public void setVisEndRange(String visEndRange) {
		this.visEndRange = visEndRange;
	}

	public String getDisplayOrderTextFileName() {
		return displayOrderTextFileName;
	}

	public void setDisplayOrderTextFileName(String displayOrderTextFileName) {
		this.displayOrderTextFileName = displayOrderTextFileName;
	}
}