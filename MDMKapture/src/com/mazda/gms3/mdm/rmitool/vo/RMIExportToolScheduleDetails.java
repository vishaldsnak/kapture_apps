package com.mazda.gms3.mdm.rmitool.vo;

import java.sql.Timestamp;
import java.util.ArrayList;

public class RMIExportToolScheduleDetails {

	private long srNo;
	private long scheduleId=0;
	private String scheduleName=null;
	private Timestamp startTime = null;
	private Timestamp finishTime=null;
	private String wslId=null;
	private long totalDocsCount=0;
	private long successDocsCount=0;
	private long failureDocsCount=0;
	private String threadId=null;
	private String reportsPath=null;
	private ArrayList<RMIExportToolItemDetails> itemsList = null;
	
	private String processingStatus=null;
	private String completionStatus=null;
	
	private long totalOkAssetsCount=0;
	private long successOkAssetsCount=0;
	private long failureOkAssetsCount=0;
	
	
	
	public long getTotalOkAssetsCount() {
		return totalOkAssetsCount;
	}
	public void setTotalOkAssetsCount(long totalOkAssetsCount) {
		this.totalOkAssetsCount = totalOkAssetsCount;
	}
	public long getSuccessOkAssetsCount() {
		return successOkAssetsCount;
	}
	public void setSuccessOkAssetsCount(long successOkAssetsCount) {
		this.successOkAssetsCount = successOkAssetsCount;
	}
	public long getFailureOkAssetsCount() {
		return failureOkAssetsCount;
	}
	public void setFailureOkAssetsCount(long failureOkAssetsCount) {
		this.failureOkAssetsCount = failureOkAssetsCount;
	}
	public long getSrNo() {
		return srNo;
	}
	public void setSrNo(long srNo) {
		this.srNo = srNo;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
	public String getCompletionStatus() {
		return completionStatus;
	}
	public void setCompletionStatus(String completionStatus) {
		this.completionStatus = completionStatus;
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
	public Timestamp getStartTime() {
		return startTime;
	}
	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}
	public Timestamp getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(Timestamp finishTime) {
		this.finishTime = finishTime;
	}
	public String getWslId() {
		return wslId;
	}
	public void setWslId(String wslId) {
		this.wslId = wslId;
	}
	public long getTotalDocsCount() {
		return totalDocsCount;
	}
	public void setTotalDocsCount(long totalDocsCount) {
		this.totalDocsCount = totalDocsCount;
	}
	public long getSuccessDocsCount() {
		return successDocsCount;
	}
	public void setSuccessDocsCount(long successDocsCount) {
		this.successDocsCount = successDocsCount;
	}
	public long getFailureDocsCount() {
		return failureDocsCount;
	}
	public void setFailureDocsCount(long failureDocsCount) {
		this.failureDocsCount = failureDocsCount;
	}
	public String getThreadId() {
		return threadId;
	}
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
	public String getReportsPath() {
		return reportsPath;
	}
	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}
	public ArrayList<RMIExportToolItemDetails> getItemsList() {
		return itemsList;
	}
	public void setItemsList(ArrayList<RMIExportToolItemDetails> itemsList) {
		this.itemsList = itemsList;
	}
	
	
}
