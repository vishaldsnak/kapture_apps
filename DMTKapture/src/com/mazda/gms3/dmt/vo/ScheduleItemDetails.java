package com.mazda.gms3.dmt.vo;

import java.util.ArrayList;


public class ScheduleItemDetails {

	private int srNo;
	private long itemId=0;
	private long scheduleId=0;
	
	private String market=null;
	private String locale=null;
	private String model=null;
	private String modelFolderName=null;
	private String manualType=null;
	private String manualTypeLabel=null;
	private String engineCode=null;
	private String engineName=null;
	private String missionCode=null;
	private String missionName=null;
	
	private long totalDocsForProcessing=0;
	private long totalDocsForDeletion=0;
	private long processedDocsCount=0;
	private long failedProcessedDocsCount=0;
	private long deletedDocsCount=0;
	private long failedDeletedDocsCount=0;
	
	private long okAssetsCount=0;
	private long okAssetsDeleteCount=0;
	private long processedOkAssetsCount=0;
	private long failedOkAssetsCount=0;
	private long processedOkAssetsDeleteCount=0;
	private long failedOkAssetsDeleteCount=0;
	
	private long totalInnerLinksCount=0;
	private long processedInnerLinksCount=0;
	private long failedInnerLinksCount=0;
	
	private String currentProcessingStatus=null;
	
	private ArrayList<ScheduleItemDetails> processingFileList = new ArrayList<ScheduleItemDetails>();
	private String sourceFileName=null;
	private String sourceFilePath=null;
	private String sourceFileExtension=null;
	private String fileNameAttribute=null;
	
	private String materialFolderName=null;
	private long metaDocsCount=0;
	
	private ArrayList<ScheduleItemDetails> okAssetsFileList = new ArrayList<ScheduleItemDetails>();
	
	private boolean rowSelected=false;
	
	private String threadId=null;
	
	
	private String scheduleName=null;
	
	/*
	 * Added for MC Market
	 */
	private String faceLiftFolderName=null;
	private String modelType=null;
	private String carlineCode=null;
	
	private long totalFilesCount=0;
	private long esiCatLeftMenuCount=0;
	
	private long totalDisplayOrderCount=0;
	private long totalCDProcessingCount=0;
	private long processedDisplayOrderCount=0;
	private long processedCDProcessingCount=0;
	private long failedDisplayOrderCount=0;
	private long failedCDProcessingCount=0;
	
	private String scheduleType=null;
	
	private long totalSCMVinCount=0;
	private long processedSCMVinCount=0;
	private long failedSCMVinCount=0;
	
	
	/*
	 * ADDED ON 25TH JULY 2019
	 * FOR BLOCKING ITEMS FOR PROCESSING OF DISPLAY ORDER IF VIN IS NOT PRESENT
	 */
	private boolean displayOrderFileFound = false;
	private boolean vinFileFound = false;
	
	private boolean allRequiredDisplayOrderExists = false;
	
	
	/*
	 * ADDED FOR IDENTIFYING WHETER ALL MATERIAL FOLDER OF THE PROCESSING FACELIFT FOLDER EXISTS
	 * ELSE NO FOLDER EXISTS - ONLY DISPLAY ORDER FILE PROVIDED
	 * TO AVOID, SITUATION - EITHER ALL MATERIAL FOLDERS MENTIONED IN VIN / DISPLAY FILE MUST BE THERE ELSE NOTHING MUST BE THERE
	 * SO THAT ALL ROWS IN DISPLAY ORDER GETS PROCESSED
	 * DATE 05 APRIL 2020
	 */
	private boolean allowProcessingMaterialFolderCheck=false;
	
	/*
	 * FLAG TO CHECK IF ALL TOKENS IN VIN TXT FILES ARE PROVIDED FOR EACH LINE
	 * IF YES = THEN ALLOW FURTHER, ELSE SKIP
	 */
	private String vinFileNamesWithIssues = null;
	
	
	
	public long getTotalSCMVinCount() {
		return totalSCMVinCount;
	}
	public void setTotalSCMVinCount(long totalSCMVinCount) {
		this.totalSCMVinCount = totalSCMVinCount;
	}
	public long getProcessedSCMVinCount() {
		return processedSCMVinCount;
	}
	public void setProcessedSCMVinCount(long processedSCMVinCount) {
		this.processedSCMVinCount = processedSCMVinCount;
	}
	public long getFailedSCMVinCount() {
		return failedSCMVinCount;
	}
	public void setFailedSCMVinCount(long failedSCMVinCount) {
		this.failedSCMVinCount = failedSCMVinCount;
	}
	public String getVinFileNamesWithIssues() {
		return vinFileNamesWithIssues;
	}
	public void setVinFileNamesWithIssues(String vinFileNamesWithIssues) {
		this.vinFileNamesWithIssues = vinFileNamesWithIssues;
	}
	public boolean isAllowProcessingMaterialFolderCheck() {
		return allowProcessingMaterialFolderCheck;
	}
	public void setAllowProcessingMaterialFolderCheck(boolean allowProcessingMaterialFolderCheck) {
		this.allowProcessingMaterialFolderCheck = allowProcessingMaterialFolderCheck;
	}
	public boolean isAllRequiredDisplayOrderExists() {
		return allRequiredDisplayOrderExists;
	}
	public void setAllRequiredDisplayOrderExists(
			boolean allRequiredDisplayOrderExists) {
		this.allRequiredDisplayOrderExists = allRequiredDisplayOrderExists;
	}
	public boolean isDisplayOrderFileFound() {
		return displayOrderFileFound;
	}
	public void setDisplayOrderFileFound(boolean displayOrderFileFound) {
		this.displayOrderFileFound = displayOrderFileFound;
	}
	public boolean isVinFileFound() {
		return vinFileFound;
	}
	public void setVinFileFound(boolean vinFileFound) {
		this.vinFileFound = vinFileFound;
	}
	public String getScheduleType() {
		return scheduleType;
	}
	public void setScheduleType(String scheduleType) {
		this.scheduleType = scheduleType;
	}
	public long getTotalDisplayOrderCount() {
		return totalDisplayOrderCount;
	}
	public void setTotalDisplayOrderCount(long totalDisplayOrderCount) {
		this.totalDisplayOrderCount = totalDisplayOrderCount;
	}
	public long getTotalCDProcessingCount() {
		return totalCDProcessingCount;
	}
	public void setTotalCDProcessingCount(long totalCDProcessingCount) {
		this.totalCDProcessingCount = totalCDProcessingCount;
	}
	public long getProcessedDisplayOrderCount() {
		return processedDisplayOrderCount;
	}
	public void setProcessedDisplayOrderCount(long processedDisplayOrderCount) {
		this.processedDisplayOrderCount = processedDisplayOrderCount;
	}
	public long getProcessedCDProcessingCount() {
		return processedCDProcessingCount;
	}
	public void setProcessedCDProcessingCount(long processedCDProcessingCount) {
		this.processedCDProcessingCount = processedCDProcessingCount;
	}
	public long getFailedDisplayOrderCount() {
		return failedDisplayOrderCount;
	}
	public void setFailedDisplayOrderCount(long failedDisplayOrderCount) {
		this.failedDisplayOrderCount = failedDisplayOrderCount;
	}
	public long getFailedCDProcessingCount() {
		return failedCDProcessingCount;
	}
	public void setFailedCDProcessingCount(long failedCDProcessingCount) {
		this.failedCDProcessingCount = failedCDProcessingCount;
	}
	public long getTotalFilesCount() {
		return totalFilesCount;
	}
	public void setTotalFilesCount(long totalFilesCount) {
		this.totalFilesCount = totalFilesCount;
	}
	public long getEsiCatLeftMenuCount() {
		return esiCatLeftMenuCount;
	}
	public void setEsiCatLeftMenuCount(long esiCatLeftMenuCount) {
		this.esiCatLeftMenuCount = esiCatLeftMenuCount;
	}
	public long getProcessedDocsCount() {
		return processedDocsCount;
	}
	public void setProcessedDocsCount(long processedDocsCount) {
		this.processedDocsCount = processedDocsCount;
	}
	public long getFailedProcessedDocsCount() {
		return failedProcessedDocsCount;
	}
	public void setFailedProcessedDocsCount(long failedProcessedDocsCount) {
		this.failedProcessedDocsCount = failedProcessedDocsCount;
	}
	public long getDeletedDocsCount() {
		return deletedDocsCount;
	}
	public void setDeletedDocsCount(long deletedDocsCount) {
		this.deletedDocsCount = deletedDocsCount;
	}
	public long getFailedDeletedDocsCount() {
		return failedDeletedDocsCount;
	}
	public void setFailedDeletedDocsCount(long failedDeletedDocsCount) {
		this.failedDeletedDocsCount = failedDeletedDocsCount;
	}
	public long getProcessedOkAssetsCount() {
		return processedOkAssetsCount;
	}
	public void setProcessedOkAssetsCount(long processedOkAssetsCount) {
		this.processedOkAssetsCount = processedOkAssetsCount;
	}
	public long getFailedOkAssetsCount() {
		return failedOkAssetsCount;
	}
	public void setFailedOkAssetsCount(long failedOkAssetsCount) {
		this.failedOkAssetsCount = failedOkAssetsCount;
	}
	public long getProcessedOkAssetsDeleteCount() {
		return processedOkAssetsDeleteCount;
	}
	public void setProcessedOkAssetsDeleteCount(long processedOkAssetsDeleteCount) {
		this.processedOkAssetsDeleteCount = processedOkAssetsDeleteCount;
	}
	public long getFailedOkAssetsDeleteCount() {
		return failedOkAssetsDeleteCount;
	}
	public void setFailedOkAssetsDeleteCount(long failedOkAssetsDeleteCount) {
		this.failedOkAssetsDeleteCount = failedOkAssetsDeleteCount;
	}
	public long getTotalInnerLinksCount() {
		return totalInnerLinksCount;
	}
	public void setTotalInnerLinksCount(long totalInnerLinksCount) {
		this.totalInnerLinksCount = totalInnerLinksCount;
	}
	public long getProcessedInnerLinksCount() {
		return processedInnerLinksCount;
	}
	public void setProcessedInnerLinksCount(long processedInnerLinksCount) {
		this.processedInnerLinksCount = processedInnerLinksCount;
	}
	public long getFailedInnerLinksCount() {
		return failedInnerLinksCount;
	}
	public void setFailedInnerLinksCount(long failedInnerLinksCount) {
		this.failedInnerLinksCount = failedInnerLinksCount;
	}
	public String getCurrentProcessingStatus() {
		return currentProcessingStatus;
	}
	public void setCurrentProcessingStatus(String currentProcessingStatus) {
		this.currentProcessingStatus = currentProcessingStatus;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getFaceLiftFolderName() {
		return faceLiftFolderName;
	}
	public void setFaceLiftFolderName(String faceLiftFolderName) {
		this.faceLiftFolderName = faceLiftFolderName;
	}
	public String getModelType() {
		return modelType;
	}
	public void setModelType(String modelType) {
		this.modelType = modelType;
	}
	public String getScheduleName() {
		return scheduleName;
	}
	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}
	public String getThreadId() {
		return threadId;
	}
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
	public String getMaterialFolderName() {
		return materialFolderName;
	}
	public void setMaterialFolderName(String materialFolderName) {
		this.materialFolderName = materialFolderName;
	}
	public boolean isRowSelected() {
		return rowSelected;
	}
	public void setRowSelected(boolean rowSelected) {
		this.rowSelected = rowSelected;
	}
	public ArrayList<ScheduleItemDetails> getOkAssetsFileList() {
		return okAssetsFileList;
	}
	public void setOkAssetsFileList(ArrayList<ScheduleItemDetails> okAssetsFileList) {
		this.okAssetsFileList = okAssetsFileList;
	}
	public String getSourceFileName() {
		return sourceFileName;
	}
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}
	public String getSourceFilePath() {
		return sourceFilePath;
	}
	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}
	public String getSourceFileExtension() {
		return sourceFileExtension;
	}
	public void setSourceFileExtension(String sourceFileExtension) {
		this.sourceFileExtension = sourceFileExtension;
	}
	public String getFileNameAttribute() {
		return fileNameAttribute;
	}
	public void setFileNameAttribute(String fileNameAttribute) {
		this.fileNameAttribute = fileNameAttribute;
	}
	public ArrayList<ScheduleItemDetails> getProcessingFileList() {
		return processingFileList;
	}
	public void setProcessingFileList(
			ArrayList<ScheduleItemDetails> processingFileList) {
		this.processingFileList = processingFileList;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
	}
	public long getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
	}
	public String getMarket() {
		return market;
	}
	public void setMarket(String market) {
		this.market = market;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getManualType() {
		return manualType;
	}
	public void setManualType(String manualType) {
		this.manualType = manualType;
	}
	public String getManualTypeLabel() {
		return manualTypeLabel;
	}
	public void setManualTypeLabel(String manualTypeLabel) {
		this.manualTypeLabel = manualTypeLabel;
	}
	public String getEngineCode() {
		return engineCode;
	}
	public void setEngineCode(String engineCode) {
		this.engineCode = engineCode;
	}
	public String getEngineName() {
		return engineName;
	}
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}
	public String getMissionCode() {
		return missionCode;
	}
	public void setMissionCode(String missionCode) {
		this.missionCode = missionCode;
	}
	public String getMissionName() {
		return missionName;
	}
	public void setMissionName(String missionName) {
		this.missionName = missionName;
	}
	public long getTotalDocsForProcessing() {
		return totalDocsForProcessing;
	}
	public void setTotalDocsForProcessing(long totalDocsForProcessing) {
		this.totalDocsForProcessing = totalDocsForProcessing;
	}
	public long getTotalDocsForDeletion() {
		return totalDocsForDeletion;
	}
	public void setTotalDocsForDeletion(long totalDocsForDeletion) {
		this.totalDocsForDeletion = totalDocsForDeletion;
	}
	public long getOkAssetsCount() {
		return okAssetsCount;
	}
	public void setOkAssetsCount(long okAssetsCount) {
		this.okAssetsCount = okAssetsCount;
	}
	public long getOkAssetsDeleteCount() {
		return okAssetsDeleteCount;
	}
	public void setOkAssetsDeleteCount(long okAssetsDeleteCount) {
		this.okAssetsDeleteCount = okAssetsDeleteCount;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public String getModelFolderName() {
		return modelFolderName;
	}
	public void setModelFolderName(String modelFolderName) {
		this.modelFolderName = modelFolderName;
	}
	public long getMetaDocsCount() {
		return metaDocsCount;
	}
	public void setMetaDocsCount(long metaDocsCount) {
		this.metaDocsCount = metaDocsCount;
	}
}