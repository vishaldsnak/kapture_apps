package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class SIInnerLinksIdentificationTransactionDetails {

	private int srNo;
	private String jobName=null;
	private Long jobId=null;
	private Long totalDocumentsCount=null;
	private Long processedDocumentsCount=null;
	private Long failureDocumentsCount=null;
	
	private Timestamp startTime=null;
	private Timestamp finishTime=null;
	
	private Long totalInnerLinksCount=null;
	private Long activeInnerLinksCount=null;
	private Long inactiveInnerLinksCount=null;
	private String jobStatus=null;
	private String processingStatus=null;
	private String reportsPath=null;

	private String reportLocale=null;
	
	
	public String getReportLocale() {
		return reportLocale;
	}

	public void setReportLocale(String reportLocale) {
		this.reportLocale = reportLocale;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public String getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public Long getTotalDocumentsCount() {
		return totalDocumentsCount;
	}

	public void setTotalDocumentsCount(Long totalDocumentsCount) {
		this.totalDocumentsCount = totalDocumentsCount;
	}

	public Long getProcessedDocumentsCount() {
		return processedDocumentsCount;
	}

	public void setProcessedDocumentsCount(Long processedDocumentsCount) {
		this.processedDocumentsCount = processedDocumentsCount;
	}

	public Long getFailureDocumentsCount() {
		return failureDocumentsCount;
	}

	public void setFailureDocumentsCount(Long failureDocumentsCount) {
		this.failureDocumentsCount = failureDocumentsCount;
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

	public Long getTotalInnerLinksCount() {
		return totalInnerLinksCount;
	}

	public void setTotalInnerLinksCount(Long totalInnerLinksCount) {
		this.totalInnerLinksCount = totalInnerLinksCount;
	}

	public Long getActiveInnerLinksCount() {
		return activeInnerLinksCount;
	}

	public void setActiveInnerLinksCount(Long activeInnerLinksCount) {
		this.activeInnerLinksCount = activeInnerLinksCount;
	}

	public Long getInactiveInnerLinksCount() {
		return inactiveInnerLinksCount;
	}

	public void setInactiveInnerLinksCount(Long inactiveInnerLinksCount) {
		this.inactiveInnerLinksCount = inactiveInnerLinksCount;
	}

	public String getJobStatus() {
		return jobStatus;
	}

	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}

	public String getReportsPath() {
		return reportsPath;
	}

	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}
	
}
