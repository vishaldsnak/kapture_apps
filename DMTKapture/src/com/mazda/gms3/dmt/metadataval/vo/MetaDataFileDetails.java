package com.mazda.gms3.dmt.metadataval.vo;

import java.sql.Timestamp;

public class MetaDataFileDetails {

	private Long scheduleId=null;
	private String scheduleName=null;
	private int srNo;
	private String locale=null;
	private String metaDataType=null;
	private String metaDataTypeLabel=null;
	private String fileName=null;
	private String filePath=null;
	private String webFilePath=null;
	private byte[] fileData=null;
	private String status=null;
	private String reportsPath=null;
	private String wslId=null;
	private int totalCount=0;
	private int successCount=0;
	private int failureCount=0;
	private Timestamp startTime=null;
	private Timestamp finishTime=null;
	private String threadId=null;
	private String schdeuleRemarks=null;
	
	private boolean showAbort=false;
	
	private boolean rowSelected = false;
	
	
	public boolean isRowSelected() {
		return rowSelected;
	}
	public void setRowSelected(boolean rowSelected) {
		this.rowSelected = rowSelected;
	}
	
	
	public String getSchdeuleRemarks() {
		return schdeuleRemarks;
	}
	public void setSchdeuleRemarks(String schdeuleRemarks) {
		this.schdeuleRemarks = schdeuleRemarks;
	}
	public Long getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(Long scheduleId) {
		this.scheduleId = scheduleId;
	}
	public String getScheduleName() {
		return scheduleName;
	}
	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getReportsPath() {
		return reportsPath;
	}
	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}
	public String getWslId() {
		return wslId;
	}
	public void setWslId(String wslId) {
		this.wslId = wslId;
	}
	public int getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
	public int getSuccessCount() {
		return successCount;
	}
	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}
	public int getFailureCount() {
		return failureCount;
	}
	public void setFailureCount(int failureCount) {
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
	public String getThreadId() {
		return threadId;
	}
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
	public boolean isShowAbort() {
		return showAbort;
	}
	public void setShowAbort(boolean showAbort) {
		this.showAbort = showAbort;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public String getMetaDataType() {
		return metaDataType;
	}
	public void setMetaDataType(String metaDataType) {
		this.metaDataType = metaDataType;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getWebFilePath() {
		return webFilePath;
	}
	public void setWebFilePath(String webFilePath) {
		this.webFilePath = webFilePath;
	}
	public byte[] getFileData() {
		return fileData;
	}
	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
	public String getMetaDataTypeLabel() {
		return metaDataTypeLabel;
	}
	public void setMetaDataTypeLabel(String metaDataTypeLabel) {
		this.metaDataTypeLabel = metaDataTypeLabel;
	}
}