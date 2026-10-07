package com.mazda.gms3.dmt.autosync.vo;

public class AutoSyncCategoryDetails {

	private long itemId=0;
	private String errorCode=null;
	private String errorMessage=null;
	private String processingStatus = null;
	private String operationType=null;
	private String market=null;
	private String itemType=null;
	
	private String locale=null;
	private String categoryName=null;
	private String categoryRefKey=null;
	private String parentRefKey=null;
	
	private String mdmItemId=null;
	
	private boolean lastLevel = false;
	
	private AutoSyncCategoryDetails level1Details = null;
	private AutoSyncCategoryDetails level2Details = null;
	private AutoSyncCategoryDetails level3Details = null;
	private AutoSyncCategoryDetails level4Details = null;
	private AutoSyncCategoryDetails level5Details = null;
	
	private String categoryLevel=null;
	
	
	public String getCategoryLevel() {
		return categoryLevel;
	}
	public void setCategoryLevel(String categoryLevel) {
		this.categoryLevel = categoryLevel;
	}
	public AutoSyncCategoryDetails getLevel1Details() {
		return level1Details;
	}
	public void setLevel1Details(AutoSyncCategoryDetails level1Details) {
		this.level1Details = level1Details;
	}
	public AutoSyncCategoryDetails getLevel2Details() {
		return level2Details;
	}
	public void setLevel2Details(AutoSyncCategoryDetails level2Details) {
		this.level2Details = level2Details;
	}
	public AutoSyncCategoryDetails getLevel3Details() {
		return level3Details;
	}
	public void setLevel3Details(AutoSyncCategoryDetails level3Details) {
		this.level3Details = level3Details;
	}
	public AutoSyncCategoryDetails getLevel4Details() {
		return level4Details;
	}
	public void setLevel4Details(AutoSyncCategoryDetails level4Details) {
		this.level4Details = level4Details;
	}
	public AutoSyncCategoryDetails getLevel5Details() {
		return level5Details;
	}
	public void setLevel5Details(AutoSyncCategoryDetails level5Details) {
		this.level5Details = level5Details;
	}
	public boolean isLastLevel() {
		return lastLevel;
	}
	public void setLastLevel(boolean lastLevel) {
		this.lastLevel = lastLevel;
	}
	public String getMdmItemId() {
		return mdmItemId;
	}
	public void setMdmItemId(String mdmItemId) {
		this.mdmItemId = mdmItemId;
	}
	public String getMarket() {
		return market;
	}
	public void setMarket(String market) {
		this.market = market;
	}
	public String getItemType() {
		return itemType;
	}
	public void setItemType(String itemType) {
		this.itemType = itemType;
	}
	public String getOperationType() {
		return operationType;
	}
	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
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
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
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
}
