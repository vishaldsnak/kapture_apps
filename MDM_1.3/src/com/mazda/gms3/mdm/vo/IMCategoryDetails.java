package com.mazda.gms3.mdm.vo;

import java.util.ArrayList;

public class IMCategoryDetails {
	
	private int srNo;
	private String categoryName = null;
	private String categoryRefKey = null;
	private String parentRefKey = null;
	private String locale = null;
	private String objectId = null;
	private String level=null;

	// required for scheduling a Job
	private SIVinDetails itemDetails =null;
	
	
	private long scheduleId;
	private long itemId;
	private String processingStatus=null;
	private String errorCode=null;
	private String errorMessage=null;
	private String operationType=null;
	
	private ArrayList<IMCategoryDetails> childList = null;
	
	private boolean modelYearCategory=false;
	
	
	
	public boolean isModelYearCategory() {
		return modelYearCategory;
	}

	public void setModelYearCategory(boolean modelYearCategory) {
		this.modelYearCategory = modelYearCategory;
	}

	public ArrayList<IMCategoryDetails> getChildList() {
		return childList;
	}

	public void setChildList(ArrayList<IMCategoryDetails> childList) {
		this.childList = childList;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public long getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
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

	public String getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}

	public long getItemId() {
		return itemId;
	}

	public void setItemId(long itemId) {
		this.itemId = itemId;
	}

	public SIVinDetails getItemDetails() {
		return itemDetails;
	}

	public void setItemDetails(SIVinDetails itemDetails) {
		this.itemDetails = itemDetails;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryRefKey() {
		return categoryRefKey;
	}

	public void setCategoryRefKey(String categoryRefKey) {
		this.categoryRefKey = categoryRefKey;
	}

	public String getParentRefKey() {
		return parentRefKey;
	}

	public void setParentRefKey(String parentRefKey) {
		this.parentRefKey = parentRefKey;
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

}
