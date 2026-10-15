package com.mazda.gms3.cdrom.bean;

import java.util.ArrayList;


public class CDRomScheduleItemDetails {

	private int srNo;
	private long itemId=0;
	private long scheduleId=0;
	
	private String market=null;
	private String locale=null;
	private String model=null;
	private String manualType=null;
	private String manualTypeLabel=null;
	private String manualTypeDisplayLabel=null;
	private String engineCode=null;
	private String engineName=null;
	private String missionCode=null;
	private String missionName=null;
	
	private long totalDocsForProcessing=0;
	private long processedDocsCount=0;
	private long failedProcessedDocsCount=0;
	
	private long okAssetsCount=0;
	private long processedOkAssetsCount=0;
	private long failedOkAssetsCount=0;
	
	private long totalInnerLinksCount=0;
	private long processedInnerLinksCount=0;
	private long failedInnerLinksCount=0;
	
	private String currentProcessingStatus=null;
	
	private ArrayList<CDRomScheduleItemDetails> processingFileList = new ArrayList<CDRomScheduleItemDetails>();
	private String sourceFileName=null;
	private String sourceFilePath=null;
	private String sourceFileExtension=null;
	private String fileNameAttribute=null;
	
	private ArrayList<CDRomScheduleItemDetails> okAssetsFileList = new ArrayList<CDRomScheduleItemDetails>();
	
	private boolean rowSelected=false;
	
	private String threadId=null;
	
	
	private String scheduleName=null;
	
	/*
	 * Added for MC Market
	 */
	private String faceLiftFolderName=null;
	private String modelType=null;
	private String carlineCode=null;
	
	private String manualTypeName = null;
	private String channelName = null;
	private String channelRefKey = null;
	
	private String manualTypeCode = null;
	
	private String wmiCode = null;
	
	
	private ArrayList<String> documentIdsList = new ArrayList<String>();
	
	
	
	public String getManualTypeDisplayLabel() {
		return manualTypeDisplayLabel;
	}
	public void setManualTypeDisplayLabel(String manualTypeDisplayLabel) {
		this.manualTypeDisplayLabel = manualTypeDisplayLabel;
	}
	public ArrayList<String> getDocumentIdsList() {
		return documentIdsList;
	}
	public void setDocumentIdsList(ArrayList<String> documentIdsList) {
		this.documentIdsList = documentIdsList;
	}
	public String getWmiCode() {
		return wmiCode;
	}
	public void setWmiCode(String wmiCode) {
		this.wmiCode = wmiCode;
	}
	public String getVdsCode() {
		return vdsCode;
	}
	public void setVdsCode(String vdsCode) {
		this.vdsCode = vdsCode;
	}
	public String getVinRange() {
		return vinRange;
	}
	public void setVinRange(String vinRange) {
		this.vinRange = vinRange;
	}
	private String vdsCode = null;
	private String vinRange = null;
	
	public String getManualTypeCode() {
		return manualTypeCode;
	}
	public void setManualTypeCode(String manualTypeCode) {
		this.manualTypeCode = manualTypeCode;
	}
	public String getManualTypeName() {
		return manualTypeName;
	}
	public void setManualTypeName(String manualTypeName) {
		this.manualTypeName = manualTypeName;
	}
	public String getChannelName() {
		return channelName;
	}
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public String getChannelRefKey() {
		return channelRefKey;
	}
	public void setChannelRefKey(String channelRefKey) {
		this.channelRefKey = channelRefKey;
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
	public boolean isRowSelected() {
		return rowSelected;
	}
	public void setRowSelected(boolean rowSelected) {
		this.rowSelected = rowSelected;
	}
	public ArrayList<CDRomScheduleItemDetails> getOkAssetsFileList() {
		return okAssetsFileList;
	}
	public void setOkAssetsFileList(ArrayList<CDRomScheduleItemDetails> okAssetsFileList) {
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
	public ArrayList<CDRomScheduleItemDetails> getProcessingFileList() {
		return processingFileList;
	}
	public void setProcessingFileList(
			ArrayList<CDRomScheduleItemDetails> processingFileList) {
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
	public long getOkAssetsCount() {
		return okAssetsCount;
	}
	public void setOkAssetsCount(long okAssetsCount) {
		this.okAssetsCount = okAssetsCount;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
}