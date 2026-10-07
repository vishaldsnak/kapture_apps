package com.mazda.gms3.dmt.autosync.vo;

import java.sql.Timestamp;
import java.util.ArrayList;

public class AutoSyncScheduleDetails {
	
	private int srNo;
	private long scheduleId;
	private String scheduleName=null;
	private String market=null;
	private Timestamp scheduleTime=null;
	private Timestamp finishTime=null;
	private String userId=null;
	private int totalCount;
	private int successCount;
	private int failureCount;
	
	private String reportsPath=null;
	private ArrayList<AutoSyncScheduleItemDetails> itemsList = null;
	private String scheduleStatus=null;
	private String jobStatus=null;
	private String threadId = null;
	
	private boolean rowSelected = false;
	
	
	public boolean isRowSelected() {
		return rowSelected;
	}
	public void setRowSelected(boolean rowSelected) {
		this.rowSelected = rowSelected;
	}
	public String getMarket() {
		return market;
	}
	public void setMarket(String market) {
		this.market = market;
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
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
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
	public String getReportsPath() {
		return reportsPath;
	}
	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}
	public ArrayList<AutoSyncScheduleItemDetails> getItemsList() {
		return itemsList;
	}
	public void setItemsList(ArrayList<AutoSyncScheduleItemDetails> itemsList) {
		this.itemsList = itemsList;
	}
	public String getScheduleStatus() {
		return scheduleStatus;
	}
	public void setScheduleStatus(String scheduleStatus) {
		this.scheduleStatus = scheduleStatus;
	}
	public String getJobStatus() {
		return jobStatus;
	}
	public void setJobStatus(String jobStatus) {
		this.jobStatus = jobStatus;
	}
	public String getThreadId() {
		return threadId;
	}
	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}
}