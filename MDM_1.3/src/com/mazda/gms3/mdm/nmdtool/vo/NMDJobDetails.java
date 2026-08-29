package com.mazda.gms3.mdm.nmdtool.vo;

import java.sql.Timestamp;

public class NMDJobDetails {

	private int srNo;
	private long jobId;
	private long attemptId;
	private String jobName=null;
	private String locale=null;
	private String model=null;
	private Timestamp startTime=null;
	private Timestamp endTime=null;
	private String runningStatus=null;
	private String jobStatus=null;
	private long totalDocs;
	private long successDoc;
	private long failureDocs;
	
	private long totalInnerLinks;
	private long successInnerLinks;
	private long failureInnnerLinks;
	
	private long totalOkAssets;
	private long successOkAssets;
	private long failureOkAssets;
	
	private long totalDelDocs;
	private long successDelDoc;
	private long failureDelDocs;
	
	private String threadId=null;
	private int attemptsCount;
	private String reportsPath=null;
	private Timestamp createdTime=null;
	private Timestamp modifiedTime=null;
	private String failureReason=null;
	
	private long totalTransferDocs;
	private long successTransferDoc;
	private long failureTransferDocs;
	private long skippedTranferDocs;
	private String notificationSent=null;
	
	private int deltaTotalCount=0;
	private int deltaSuccessCount=0;
	private int deltaFailureCount=0;
	private int deltaSkippedCount=0;
	private int deltaDeleteSuccessCount=0;
	private int deltaDeleteFailureCount=0;
	private int deltaDeleteTotalCount=0;
	
	private int fullTotalCount=0;
	private int fullSuccessCount=0;
	private int fullFailureCount=0;
	private int fullSkippedCount=0;
	private int fullDeleteSuccessCount=0;
	private int fullDeleteFailureCount=0;
	private int fullDeleteTotalCount=0;
	
	
	public int getDeltaTotalCount() {
		return deltaTotalCount;
	}
	public void setDeltaTotalCount(int deltaTotalCount) {
		this.deltaTotalCount = deltaTotalCount;
	}
	public int getDeltaDeleteTotalCount() {
		return deltaDeleteTotalCount;
	}
	public void setDeltaDeleteTotalCount(int deltaDeleteTotalCount) {
		this.deltaDeleteTotalCount = deltaDeleteTotalCount;
	}
	public int getFullTotalCount() {
		return fullTotalCount;
	}
	public void setFullTotalCount(int fullTotalCount) {
		this.fullTotalCount = fullTotalCount;
	}
	public int getFullDeleteTotalCount() {
		return fullDeleteTotalCount;
	}
	public void setFullDeleteTotalCount(int fullDeleteTotalCount) {
		this.fullDeleteTotalCount = fullDeleteTotalCount;
	}
	public long getSkippedTranferDocs() {
		return skippedTranferDocs;
	}
	public void setSkippedTranferDocs(long skippedTranferDocs) {
		this.skippedTranferDocs = skippedTranferDocs;
	}
	public int getDeltaSuccessCount() {
		return deltaSuccessCount;
	}
	public void setDeltaSuccessCount(int deltaSuccessCount) {
		this.deltaSuccessCount = deltaSuccessCount;
	}
	public int getDeltaFailureCount() {
		return deltaFailureCount;
	}
	public void setDeltaFailureCount(int deltaFailureCount) {
		this.deltaFailureCount = deltaFailureCount;
	}
	public int getDeltaSkippedCount() {
		return deltaSkippedCount;
	}
	public void setDeltaSkippedCount(int deltaSkippedCount) {
		this.deltaSkippedCount = deltaSkippedCount;
	}
	public int getDeltaDeleteSuccessCount() {
		return deltaDeleteSuccessCount;
	}
	public void setDeltaDeleteSuccessCount(int deltaDeleteSuccessCount) {
		this.deltaDeleteSuccessCount = deltaDeleteSuccessCount;
	}
	public int getDeltaDeleteFailureCount() {
		return deltaDeleteFailureCount;
	}
	public void setDeltaDeleteFailureCount(int deltaDeleteFailureCount) {
		this.deltaDeleteFailureCount = deltaDeleteFailureCount;
	}
	public int getFullSuccessCount() {
		return fullSuccessCount;
	}
	public void setFullSuccessCount(int fullSuccessCount) {
		this.fullSuccessCount = fullSuccessCount;
	}
	public int getFullFailureCount() {
		return fullFailureCount;
	}
	public void setFullFailureCount(int fullFailureCount) {
		this.fullFailureCount = fullFailureCount;
	}
	public int getFullSkippedCount() {
		return fullSkippedCount;
	}
	public void setFullSkippedCount(int fullSkippedCount) {
		this.fullSkippedCount = fullSkippedCount;
	}
	public int getFullDeleteSuccessCount() {
		return fullDeleteSuccessCount;
	}
	public void setFullDeleteSuccessCount(int fullDeleteSuccessCount) {
		this.fullDeleteSuccessCount = fullDeleteSuccessCount;
	}
	public int getFullDeleteFailureCount() {
		return fullDeleteFailureCount;
	}
	public void setFullDeleteFailureCount(int fullDeleteFailureCount) {
		this.fullDeleteFailureCount = fullDeleteFailureCount;
	}
	public long getTotalTransferDocs() {
		return totalTransferDocs;
	}
	public void setTotalTransferDocs(long totalTransferDocs) {
		this.totalTransferDocs = totalTransferDocs;
	}
	public long getSuccessTransferDoc() {
		return successTransferDoc;
	}
	public void setSuccessTransferDoc(long successTransferDoc) {
		this.successTransferDoc = successTransferDoc;
	}
	public long getFailureTransferDocs() {
		return failureTransferDocs;
	}
	public void setFailureTransferDocs(long failureTransferDocs) {
		this.failureTransferDocs = failureTransferDocs;
	}
	public String getNotificationSent() {
		return notificationSent;
	}
	public void setNotificationSent(String notificationSent) {
		this.notificationSent = notificationSent;
	}
	public String getFailureReason() {
		return failureReason;
	}
	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}
	public long getTotalDelDocs() {
		return totalDelDocs;
	}
	public void setTotalDelDocs(long totalDelDocs) {
		this.totalDelDocs = totalDelDocs;
	}
	public long getSuccessDelDoc() {
		return successDelDoc;
	}
	public void setSuccessDelDoc(long successDelDoc) {
		this.successDelDoc = successDelDoc;
	}
	public long getFailureDelDocs() {
		return failureDelDocs;
	}
	public void setFailureDelDocs(long failureDelDocs) {
		this.failureDelDocs = failureDelDocs;
	}
	public long getAttemptId() {
		return attemptId;
	}
	public void setAttemptId(long attemptId) {
		this.attemptId = attemptId;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public long getJobId() {
		return jobId;
	}
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public Timestamp getStartTime() {
		return startTime;
	}
	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}
	public Timestamp getEndTime() {
		return endTime;
	}
	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}
	public String getRunningStatus() {
		return runningStatus;
	}
	public void setRunningStatus(String runningStatus) {
		this.runningStatus = runningStatus;
	}
	public String getJobStatus() {
		return jobStatus;
	}
	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}
	public long getTotalDocs() {
		return totalDocs;
	}
	public void setTotalDocs(long totalDocs) {
		this.totalDocs = totalDocs;
	}
	public long getSuccessDoc() {
		return successDoc;
	}
	public void setSuccessDoc(long successDoc) {
		this.successDoc = successDoc;
	}
	public long getFailureDocs() {
		return failureDocs;
	}
	public void setFailureDocs(long failureDocs) {
		this.failureDocs = failureDocs;
	}
	public long getTotalInnerLinks() {
		return totalInnerLinks;
	}
	public void setTotalInnerLinks(long totalInnerLinks) {
		this.totalInnerLinks = totalInnerLinks;
	}
	public long getSuccessInnerLinks() {
		return successInnerLinks;
	}
	public void setSuccessInnerLinks(long successInnerLinks) {
		this.successInnerLinks = successInnerLinks;
	}
	public long getFailureInnnerLinks() {
		return failureInnnerLinks;
	}
	public void setFailureInnnerLinks(long failureInnnerLinks) {
		this.failureInnnerLinks = failureInnnerLinks;
	}
	public long getTotalOkAssets() {
		return totalOkAssets;
	}
	public void setTotalOkAssets(long totalOkAssets) {
		this.totalOkAssets = totalOkAssets;
	}
	public long getSuccessOkAssets() {
		return successOkAssets;
	}
	public void setSuccessOkAssets(long successOkAssets) {
		this.successOkAssets = successOkAssets;
	}
	public long getFailureOkAssets() {
		return failureOkAssets;
	}
	public void setFailureOkAssets(long failureOkAssets) {
		this.failureOkAssets = failureOkAssets;
	}
	public String getThreadId() {
		return threadId;
	}
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
	public int getAttemptsCount() {
		return attemptsCount;
	}
	public void setAttemptsCount(int attemptsCount) {
		this.attemptsCount = attemptsCount;
	}
	public String getReportsPath() {
		return reportsPath;
	}
	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}
	public Timestamp getCreatedTime() {
		return createdTime;
	}
	public void setCreatedTime(Timestamp createdTime) {
		this.createdTime = createdTime;
	}
	public Timestamp getModifiedTime() {
		return modifiedTime;
	}
	public void setModifiedTime(Timestamp modifiedTime) {
		this.modifiedTime = modifiedTime;
	}
}