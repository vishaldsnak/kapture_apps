package com.mazda.gms3.dmt.autosync.vo;

import java.util.ArrayList;

public class AutoSyncScheduleItemDetails {

	private int srNo;
	private long itemId;
	private long scheduleId;
	private String locale=null;
	private String itemKey=null;
	private String itemName=null;
	private int totalCount;
	private int successCount;
	private int failureCount;
	private String processingStatus=null;
	
	private ArrayList<AutoSyncCategoryDetails> categoryList = null;
	
	
	public String getItemKey() {
		return itemKey;
	}
	public void setItemKey(String itemKey) {
		this.itemKey = itemKey;
	}
	public ArrayList<AutoSyncCategoryDetails> getCategoryList() {
		return categoryList;
	}
	public void setCategoryList(ArrayList<AutoSyncCategoryDetails> categoryList) {
		this.categoryList = categoryList;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
	}
	public long getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
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
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
}