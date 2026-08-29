package com.mazda.gms3.cdrom.bean;

import java.sql.Timestamp;
import java.util.ArrayList;

public class CDRomScheduleDetails {

	private int srNo;
	private long scheduleId = 0;
	private String scheduleName = null;
	private String userId = null;
	private String imDocsProcessingStatus = null;
	private long countryLocalId = 0;
	

	private long languageLocalId = 0;
	private String localeCode = null;
	private String carlineCode = null;
	private String carlineNameRegional = null;
	private String modelType = null;
	private long vinId = 0;
	private String vinWmiCode = null;
	private String vinVdsCode = null;
	private String vinStartRange= null;
	
	private long totalDocsForProcessing = 0;
	private long processedDocsCount = 0;
	private long failedProcessedDocsCount = 0;

	private Timestamp scheduleTime = null;
	private Timestamp finishTime = null;

	private long okAssetsCount = 0;
	private long processedOkAssetsCount = 0;
	private long failedOkAssetsCount = 0;

	private String scheduleStatus = null;
	private String scheduleStatusLabel = null;

	private ArrayList<CDRomScheduleItemDetails> itemsList = new ArrayList<CDRomScheduleItemDetails>();
	private String threadId = null;
	private boolean showAbort = false;

	private String innerLinksProcessingStatus = null;
	private long totalInnerLinksCount = 0;
	private long processedInnerLinksCount = 0;
	private long failedInnerLinksCount = 0;

	private String currentProcessingStatus = null;
	private String marketName = null;

	private String zipFileName = null;
	
	private boolean showViewLink=false;
	
	private String viewLinkPath=null;
	
	public String getViewLinkPath() {
		return viewLinkPath;
	}

	public void setViewLinkPath(String viewLinkPath) {
		this.viewLinkPath = viewLinkPath;
	}

	public boolean isShowViewLink() {
		return showViewLink;
	}

	public void setShowViewLink(boolean showViewLink) {
		this.showViewLink = showViewLink;
	}

	public String getZipFileName() {
		return zipFileName;
	}

	public void setZipFileName(String zipFileName) {
		this.zipFileName = zipFileName;
	}

	public long getCountryLocalId() {
		return countryLocalId;
	}

	public void setCountryLocalId(long countryLocalId) {
		this.countryLocalId = countryLocalId;
	}

	public long getLanguageLocalId() {
		return languageLocalId;
	}

	public void setLanguageLocalId(long languageLocalId) {
		this.languageLocalId = languageLocalId;
	}

	public String getLocaleCode() {
		return localeCode;
	}

	public void setLocaleCode(String localeCode) {
		this.localeCode = localeCode;
	}

	public String getCarlineCode() {
		return carlineCode;
	}

	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}

	public String getCarlineNameRegional() {
		return carlineNameRegional;
	}

	public void setCarlineNameRegional(String carlineNameRegional) {
		this.carlineNameRegional = carlineNameRegional;
	}

	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

	public long getVinId() {
		return vinId;
	}

	public void setVinId(long vinId) {
		this.vinId = vinId;
	}

	public String getVinWmiCode() {
		return vinWmiCode;
	}

	public void setVinWmiCode(String vinWmiCode) {
		this.vinWmiCode = vinWmiCode;
	}

	public String getVinVdsCode() {
		return vinVdsCode;
	}

	public void setVinVdsCode(String vinVdsCode) {
		this.vinVdsCode = vinVdsCode;
	}

	public String getVinStartRange() {
		return vinStartRange;
	}

	public void setVinStartRange(String vinStartRange) {
		this.vinStartRange = vinStartRange;
	}
	
	public String getMarketName() {
		return marketName;
	}

	public void setMarketName(String marketName) {
		this.marketName = marketName;
	}

	public String getCurrentProcessingStatus() {
		return currentProcessingStatus;
	}

	public void setCurrentProcessingStatus(String currentProcessingStatus) {
		this.currentProcessingStatus = currentProcessingStatus;
	}

	public String getInnerLinksProcessingStatus() {
		return innerLinksProcessingStatus;
	}

	public void setInnerLinksProcessingStatus(String innerLinksProcessingStatus) {
		this.innerLinksProcessingStatus = innerLinksProcessingStatus;
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
	
	public boolean isShowAbort() {
		return showAbort;
	}

	public void setShowAbort(boolean showAbort) {
		this.showAbort = showAbort;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public ArrayList<CDRomScheduleItemDetails> getItemsList() {
		return itemsList;
	}

	public void setItemsList(ArrayList<CDRomScheduleItemDetails> itemsList) {
		this.itemsList = itemsList;
	}

	public String getImDocsProcessingStatus() {
		return imDocsProcessingStatus;
	}

	public void setImDocsProcessingStatus(String imDocsProcessingStatus) {
		this.imDocsProcessingStatus = imDocsProcessingStatus;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public long getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public long getTotalDocsForProcessing() {
		return totalDocsForProcessing;
	}

	public void setTotalDocsForProcessing(long totalDocsForProcessing) {
		this.totalDocsForProcessing = totalDocsForProcessing;
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

	public Timestamp getScheduleTime() {
		return scheduleTime;
	}

	public void setScheduleTime(Timestamp scheduleTime) {
		this.scheduleTime = scheduleTime;
	}

	public Timestamp getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(Timestamp finishTime) {
		this.finishTime = finishTime;
	}

	public long getOkAssetsCount() {
		return okAssetsCount;
	}

	public void setOkAssetsCount(long okAssetsCount) {
		this.okAssetsCount = okAssetsCount;
	}

	public String getScheduleStatus() {
		return scheduleStatus;
	}

	public void setScheduleStatus(String scheduleStatus) {
		this.scheduleStatus = scheduleStatus;
	}

	public String getScheduleStatusLabel() {
		return scheduleStatusLabel;
	}

	public void setScheduleStatusLabel(String scheduleStatusLabel) {
		this.scheduleStatusLabel = scheduleStatusLabel;
	}

}
