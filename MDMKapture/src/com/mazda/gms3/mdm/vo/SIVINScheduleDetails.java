package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;
import java.util.ArrayList;


public class SIVINScheduleDetails {

	private int srNo;
	private String wslId=null;
	private String locale=null;
	private String documentId=null;
	private int totalCount=0;
	private int processedCount=0;
	private int failureCount=0;
	
	private Timestamp scheduleTime = null;
	private Timestamp finishTime = null;
	
	private String reportsPath = null;
	
	private String jobStatus=null;
	private String scheduleStatus=null;
	
	private String threadId=null;
	private String scheduleName=null;
	private long scheduleId;
	
	private ArrayList<IMCategoryDetails> categoryList = new ArrayList<IMCategoryDetails>();

	private String errorCode=null;
	private String errorMessage=null;
	private String imProcessingStatus=null;
	private String fetchedVersion=null;
	private String updatedVersion=null;
	private boolean documentPublishedStatus=false;
	private String contentId=null;
	
	private String dbprocessingStatus=null;
	
	/*
	 * THE InfoManager ContentRecordITO THAT USED TO HANG HERE IS GONE. It was set only by the MC
	 * SI VIN translation sub-flow, which was never reachable - both MCSIVin entry points passed a
	 * hardcoded null for the translation flag - and was removed with the rest of the InfoManager
	 * code. Nothing reads a content record off this bean any more: the Kapture path carries the
	 * document through SIVinDetails and KaptureContentServiceImpl instead.
	 */

	public String getDbprocessingStatus() {
		return dbprocessingStatus;
	}

	public void setDbprocessingStatus(String dbprocessingStatus) {
		this.dbprocessingStatus = dbprocessingStatus;
	}

	public String getUpdatedVersion() {
		return updatedVersion;
	}

	public void setUpdatedVersion(String updatedVersion) {
		this.updatedVersion = updatedVersion;
	}

	public boolean isDocumentPublishedStatus() {
		return documentPublishedStatus;
	}

	public void setDocumentPublishedStatus(boolean documentPublishedStatus) {
		this.documentPublishedStatus = documentPublishedStatus;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public String getFetchedVersion() {
		return fetchedVersion;
	}

	public void setFetchedVersion(String fetchedVersion) {
		this.fetchedVersion = fetchedVersion;
	}

	public String getImProcessingStatus() {
		return imProcessingStatus;
	}

	public void setImProcessingStatus(String imProcessingStatus) {
		this.imProcessingStatus = imProcessingStatus;
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

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public String getWslId() {
		return wslId;
	}

	public void setWslId(String wslId) {
		this.wslId = wslId;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public int getProcessedCount() {
		return processedCount;
	}

	public void setProcessedCount(int processedCount) {
		this.processedCount = processedCount;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
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

	public String getReportsPath() {
		return reportsPath;
	}

	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}

	public String getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}

	public String getScheduleStatus() {
		return scheduleStatus;
	}

	public void setScheduleStatus(String scheduleStatus) {
		this.scheduleStatus = scheduleStatus;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public long getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
	}

	public ArrayList<IMCategoryDetails> getCategoryList() {
		return categoryList;
	}

	public void setCategoryList(ArrayList<IMCategoryDetails> categoryList) {
		this.categoryList = categoryList;
	}
	
	
}
