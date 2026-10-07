package com.mazda.gms3.dmt.vo;

public class ReportsSummaryDetails {

	public static final String REPORT_TRANSACTION="TRANSACTION REPORT";
	public static final String REPORT_INNERLINKS="INNERLINKS REPORT";
	public static final String REPORT_OTHER_MANUALLINKS="OTHER MANUAL LINKS REPORT";
	public static final String REPORT_VIEWCONTENT_MODEL_YEAR="VIEW CONTENT MODEL YEAR REPORT";
	public static final String REPORT_VIEWCONTENT_VIN="VIEW CONTENT VIN REPORT";
	public static final String REPORT_VIEWCONTENT_MC="VIEW CONTENT REPORT";
	public static final String REPORT_DISPLAYORDER="DISPLAY ORDER REPORT";
	public static final String REPORT_FAILURE="FAILURE REPORT";
	public static final String REPORT_MISSINGCATEGORIES="MISSING CATEGORIES REPORT";
	public static final String REPORT_CD_PROCESSING="CD PROCESSING REPORT";
	public static final String REPORT_WINDOW_JS="WINDOW JS REPORT";
	public static final String REPORT_VIN_ML_MAPPING="VIN MANUAL TYPE MAPPING REPORT";
	public static final String REPORT_SCM_VIN_MAPPING="SCM VIN MAPPING REPORT";
	
	public static final String REPORT_VOLTAGE_MAP_JS="VOLTAGE MAP JS REPORT";
	public static final String REPORT_VOLTAGE_LINK_MAP_JS="VOLTAGE LINK MAP JS REPORT";
	public static final String REPORT_MASTER_DATA_LOAD="MASTER DATA LOAD REPORT";
	public static final String REPORT_MASTER_DATA_CATEGORIES="MASTER DATA CATEGORY REPORT";
	public static final String REPORT_PUBLISH="PUBLISH REPORT";
	
	public static final String STATUS_SUCCESS="SUCCESS";
	public static final String STATUS_FAILURE="FAILURE";
	
	private String schduleCode=null;
	private String reportName=null;
	private String reportStatus=null;
	private Long totalCount=null;
	private Long failureCount=null;
	private Long successCount=null;
	
	
	
	public String getSchduleCode() {
		return schduleCode;
	}
	public void setSchduleCode(String schduleCode) {
		this.schduleCode = schduleCode;
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
	public Long getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(Long totalCount) {
		this.totalCount = totalCount;
	}
	public Long getFailureCount() {
		return failureCount;
	}
	public void setFailureCount(Long failureCount) {
		this.failureCount = failureCount;
	}
	public Long getSuccessCount() {
		return successCount;
	}
	public void setSuccessCount(Long successCount) {
		this.successCount = successCount;
	}
}