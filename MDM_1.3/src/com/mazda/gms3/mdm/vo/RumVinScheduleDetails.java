package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class RumVinScheduleDetails {
	
	private int srNo;
	private String jobName=null;
	private Long jobId=null;
	
	private Long totalLinesCount=null;
	private Long validLinesCount=null;
	private Long successCount=null;
	private Long failureCount=null;
	
	private Timestamp startTime=null;
	private Timestamp finishTime=null;
	
	private String jobStatus=null;
	private String processingStatus=null;
	private String reportsPath=null;
	
	private String remakrs=null;
	private String fileName=null;
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
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
	public Long getTotalLinesCount() {
		return totalLinesCount;
	}
	public void setTotalLinesCount(Long totalLinesCount) {
		this.totalLinesCount = totalLinesCount;
	}
	public Long getValidLinesCount() {
		return validLinesCount;
	}
	public void setValidLinesCount(Long validLinesCount) {
		this.validLinesCount = validLinesCount;
	}
	public Long getSuccessCount() {
		return successCount;
	}
	public void setSuccessCount(Long successCount) {
		this.successCount = successCount;
	}
	public Long getFailureCount() {
		return failureCount;
	}
	public void setFailureCount(Long failureCount) {
		this.failureCount = failureCount;
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
	public String getJobStatus() {
		return jobStatus;
	}
	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
	public String getReportsPath() {
		return reportsPath;
	}
	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}
	public String getRemakrs() {
		return remakrs;
	}
	public void setRemakrs(String remakrs) {
		this.remakrs = remakrs;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	

	
}
