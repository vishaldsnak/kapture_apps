package com.mazda.gms3.dmt.vo;

import java.sql.Timestamp;
import java.util.ArrayList;

import com.mazda.gms3.dmt.automation.vo.ItemDetails;

public class ScheduleDetails {

	private int srNo;
	private long scheduleId=0;
	private String scheduleName=null;
	private String userId=null;
	private String imDocsProcessingStatus=null;
	
	private long totalDocsForProcessing=0;
	private long totalDocsForDeletion=0;
	private long processedDocsCount=0;
	private long failedProcessedDocsCount=0;
	private long deletedDocsCount=0;
	private long failedDeletedDocsCount=0;
	
	private Timestamp scheduleTime=null;
	private Timestamp finishTime=null;
	
	private long okAssetsCount=0;
	private long processedOkAssetsCount=0;
	private long failedOkAssetsCount=0;
	private long okAssetsDeleteCount=0;
	private long processedOkAssetsDeleteCount=0;
	private long failedOkAssetsDeleteCount=0;
	
	private String scheduleStatus=null;
	private String scheduleStatusLabel=null;
	
	private ArrayList<ScheduleItemDetails> itemsList =new ArrayList<ScheduleItemDetails>();
	private String threadId=null;
	private boolean showAbort=false;
	
	
	private long totalMetaDataDocsCount=0;
	private long processedMetaDataDocsCount=0;
	private long failedMetaDataDocsCount=0;
	
	private String innerLinksProcessingStatus=null;
	private long totalInnerLinksCount=0;
	private long processedInnerLinksCount=0;
	private long failedInnerLinksCount=0;
	
	private String currentProcessingStatus=null;
	private String marketName=null;
	
	private long totalDisplayOrderCount=0;
	private long totalCDProcessingCount=0;
	private long processedDisplayOrderCount=0;
	private long processedCDProcessingCount=0;
	private long failedDisplayOrderCount=0;
	private long failedCDProcessingCount=0;
	
	private String scheduleType=null; 
	private ArrayList<ItemDetails> automationItemsList=null;
	
	private boolean rowSelected = false;
	
	private long totalSCMVinCount=0;
	private long processedSCMVinCount=0;
	private long failedSCMVinCount=0;
	
	private String remarks=null;
	private String jobStatus=null;
	private String jobStatusLabel=null;
	private boolean showViewButton=false;
	/** MC job with an offline preview holding at least one unpublished document. */
	private boolean showPreview=false;

	public boolean isShowPreview() {
		return showPreview;
	}

	public void setShowPreview(boolean showPreview) {
		this.showPreview = showPreview;
	}
	
	public boolean isShowViewButton() {
		return showViewButton;
	}
	public void setShowViewButton(boolean showViewButton) {
		this.showViewButton = showViewButton;
	}
	public String getJobStatus() {
		return jobStatus;
	}
	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}
	public String getJobStatusLabel() {
		return jobStatusLabel;
	}
	public void setJobStatusLabel(String jobStatusLabel) {
		this.jobStatusLabel = jobStatusLabel;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
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
	public boolean isRowSelected() {
		return rowSelected;
	}
	public void setRowSelected(boolean rowSelected) {
		this.rowSelected = rowSelected;
	}
	public ArrayList<ItemDetails> getAutomationItemsList() {
		return automationItemsList;
	}
	public void setAutomationItemsList(ArrayList<ItemDetails> automationItemsList) {
		this.automationItemsList = automationItemsList;
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
	public long getFailedMetaDataDocsCount() {
		return failedMetaDataDocsCount;
	}
	public void setFailedMetaDataDocsCount(long failedMetaDataDocsCount) {
		this.failedMetaDataDocsCount = failedMetaDataDocsCount;
	}
	public long getTotalMetaDataDocsCount() {
		return totalMetaDataDocsCount;
	}
	public void setTotalMetaDataDocsCount(long totalMetaDataDocsCount) {
		this.totalMetaDataDocsCount = totalMetaDataDocsCount;
	}
	public long getProcessedMetaDataDocsCount() {
		return processedMetaDataDocsCount;
	}
	public void setProcessedMetaDataDocsCount(long processedMetaDataDocsCount) {
		this.processedMetaDataDocsCount = processedMetaDataDocsCount;
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
	public ArrayList<ScheduleItemDetails> getItemsList() {
		return itemsList;
	}
	public void setItemsList(ArrayList<ScheduleItemDetails> itemsList) {
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
	public long getTotalDocsForDeletion() {
		return totalDocsForDeletion;
	}
	public void setTotalDocsForDeletion(long totalDocsForDeletion) {
		this.totalDocsForDeletion = totalDocsForDeletion;
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
	public long getOkAssetsDeleteCount() {
		return okAssetsDeleteCount;
	}
	public void setOkAssetsDeleteCount(long okAssetsDeleteCount) {
		this.okAssetsDeleteCount = okAssetsDeleteCount;
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
