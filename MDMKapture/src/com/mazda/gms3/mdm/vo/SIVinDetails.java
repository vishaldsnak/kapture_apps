package com.mazda.gms3.mdm.vo;

import java.util.ArrayList;


public class SIVinDetails {

	private int srNo;
	private String countryLocaleId=null;
	private String manualLanguageId=null;
	private String documentId=null;
	private String locale=null;
	private String wmiCode=null;
	private String carlineCode=null;
	private String model=null;
	// used For Japan
	private String modelRegionalName=null;
	private String year=null;
	private String vdsCode=null;
	private String vinStartRange=null;
	private String vinEndRange=null;
	private String referenceKey=null;
	
	private boolean selectedRow=false;
	private boolean documentPublished=true;
	
	private String fetchedVersion=null;
	private String updatedVersion=null;
	private String wslId=null;
	
	private String firstLevelName=null;
	private String firstLevelRefKey=null;
	private String secondLevelName=null;
	private String seconddLevelRefKey=null;
	private String thirdLevelName=null;
	private String thirdLevelRefKey=null;
	private String fourthLevelName=null;
	private String fourthLevelRefKey = null;
	private String fifthLevelName=null;
	private String fifthLevelRefKey = null;
	
	private ArrayList<SIVinDetails> itemsList = null;
	
	private boolean selected = false;
	
	private String errorCode=null;
	
	/* What the server actually said, alongside the code - see KaptureContentServiceImpl. */
	private String errorMessage=null;
	
	private boolean checkedOut = false; 
	private String recordId=null;
	private ArrayList<IMCategoryDetails> categoryList = null;
	
	// Used for MME
	private String engineTypeCode=null;
	private String missionTypeCode=null;
	
	// USED FOR MME - 23 RD JUNE
	private boolean customVDS = false;
	
	
	
	public boolean isCustomVDS() {
		return customVDS;
	}
	public void setCustomVDS(boolean customVDS) {
		this.customVDS = customVDS;
	}
	public String getEngineTypeCode() {
		return engineTypeCode;
	}
	public void setEngineTypeCode(String engineTypeCode) {
		this.engineTypeCode = engineTypeCode;
	}
	public String getMissionTypeCode() {
		return missionTypeCode;
	}
	public void setMissionTypeCode(String missionTypeCode) {
		this.missionTypeCode = missionTypeCode;
	}
	public ArrayList<IMCategoryDetails> getCategoryList() {
		return categoryList;
	}
	public void setCategoryList(ArrayList<IMCategoryDetails> categoryList) {
		this.categoryList = categoryList;
	}
	public String getRecordId() {
		return recordId;
	}
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}
	public boolean isCheckedOut() {
		return checkedOut;
	}
	public void setCheckedOut(boolean checkedOut) {
		this.checkedOut = checkedOut;
	}
	public String getModelRegionalName() {
		return modelRegionalName;
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
	public void setModelRegionalName(String modelRegionalName) {
		this.modelRegionalName = modelRegionalName;
	}
	public String getFourthLevelName() {
		return fourthLevelName;
	}
	public void setFourthLevelName(String fourthLevelName) {
		this.fourthLevelName = fourthLevelName;
	}
	public String getFourthLevelRefKey() {
		return fourthLevelRefKey;
	}
	public void setFourthLevelRefKey(String fourthLevelRefKey) {
		this.fourthLevelRefKey = fourthLevelRefKey;
	}
	public String getFifthLevelName() {
		return fifthLevelName;
	}
	public void setFifthLevelName(String fifthLevelName) {
		this.fifthLevelName = fifthLevelName;
	}
	public String getFifthLevelRefKey() {
		return fifthLevelRefKey;
	}
	public void setFifthLevelRefKey(String fifthLevelRefKey) {
		this.fifthLevelRefKey = fifthLevelRefKey;
	}
	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	public String getFirstLevelName() {
		return firstLevelName;
	}
	public void setFirstLevelName(String firstLevelName) {
		this.firstLevelName = firstLevelName;
	}
	public String getSecondLevelName() {
		return secondLevelName;
	}
	public void setSecondLevelName(String secondLevelName) {
		this.secondLevelName = secondLevelName;
	}
	public String getThirdLevelName() {
		return thirdLevelName;
	}
	public void setThirdLevelName(String thirdLevelName) {
		this.thirdLevelName = thirdLevelName;
	}
	public ArrayList<SIVinDetails> getItemsList() {
		return itemsList;
	}
	public void setItemsList(ArrayList<SIVinDetails> itemsList) {
		this.itemsList = itemsList;
	}
	public String getFirstLevelRefKey() {
		return firstLevelRefKey;
	}
	public void setFirstLevelRefKey(String firstLevelRefKey) {
		this.firstLevelRefKey = firstLevelRefKey;
	}
	public String getSeconddLevelRefKey() {
		return seconddLevelRefKey;
	}
	public void setSeconddLevelRefKey(String seconddLevelRefKey) {
		this.seconddLevelRefKey = seconddLevelRefKey;
	}
	public String getThirdLevelRefKey() {
		return thirdLevelRefKey;
	}
	public void setThirdLevelRefKey(String thirdLevelRefKey) {
		this.thirdLevelRefKey = thirdLevelRefKey;
	}
	public String getWslId() {
		return wslId;
	}
	public void setWslId(String wslId) {
		this.wslId = wslId;
	}
	public String getFetchedVersion() {
		return fetchedVersion;
	}
	public void setFetchedVersion(String fetchedVersion) {
		this.fetchedVersion = fetchedVersion;
	}
	public String getUpdatedVersion() {
		return updatedVersion;
	}
	public void setUpdatedVersion(String updatedVersion) {
		this.updatedVersion = updatedVersion;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getReferenceKey() {
		return referenceKey;
	}
	public void setReferenceKey(String referenceKey) {
		this.referenceKey = referenceKey;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public boolean isDocumentPublished() {
		return documentPublished;
	}
	public void setDocumentPublished(boolean documentPublished) {
		this.documentPublished = documentPublished;
	}
	public boolean isSelectedRow() {
		return selectedRow;
	}
	public void setSelectedRow(boolean selectedRow) {
		this.selectedRow = selectedRow;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getWmiCode() {
		return wmiCode;
	}
	public void setWmiCode(String wmiCode) {
		this.wmiCode = wmiCode;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getVdsCode() {
		return vdsCode;
	}
	public void setVdsCode(String vdsCode) {
		this.vdsCode = vdsCode;
	}
	public String getVinStartRange() {
		return vinStartRange;
	}
	public void setVinStartRange(String vinStartRange) {
		this.vinStartRange = vinStartRange;
	}
	public String getVinEndRange() {
		return vinEndRange;
	}
	public void setVinEndRange(String vinEndRange) {
		this.vinEndRange = vinEndRange;
	}
	public String getCountryLocaleId() {
		return countryLocaleId;
	}
	public void setCountryLocaleId(String countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
	}
	public String getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(String manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
	}
}