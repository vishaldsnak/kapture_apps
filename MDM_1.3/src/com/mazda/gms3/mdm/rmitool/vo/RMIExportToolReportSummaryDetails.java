package com.mazda.gms3.mdm.rmitool.vo;

public class RMIExportToolReportSummaryDetails {
	
	private int srNo;
	private String scheduleId=null;
	private String reportName=null;
	private String reportStatus=null;
	private long totalCount;
	private long successCount;
	private long failureCount;
	
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public String getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}
	public String getReportName() {
		return reportName;
	}
	public void setReportName(String reportName) {
		this.reportName = reportName;
	}
	public String getReportStatus() {
		return reportStatus;
	}
	public void setReportStatus(String reportStatus) {
		this.reportStatus = reportStatus;
	}
	public long getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}
	public long getSuccessCount() {
		return successCount;
	}
	public void setSuccessCount(long successCount) {
		this.successCount = successCount;
	}
	public long getFailureCount() {
		return failureCount;
	}
	public void setFailureCount(long failureCount) {
		this.failureCount = failureCount;
	}
}