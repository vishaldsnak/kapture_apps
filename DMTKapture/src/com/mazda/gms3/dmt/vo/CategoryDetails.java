package com.mazda.gms3.dmt.vo;

import java.util.ArrayList;


public class CategoryDetails {

	private String filePath=null;
	private String categoryName=null;
	private String categoryRefKey=null;
	private String categoryGuid=null;
	private String operationType=null;
	private String categoryType=null;
	
	/*
	 * TO BE USED FOR DELETE 
	 */
	private boolean categoryDeleted= false;
	private ContentDetails contentDetails = new ContentDetails();
	
	/*
	 * ADDED ON 2ND JUNE 2018 TO IDENITFY FOR WHAT ALL MODELS YEAR INFO WAS NOT AVAILABLE 
	 * FOR EACH DOCUMENT SO THAT THE JOB CAN BE SET AS FAILURE
	 */
	private String model=null;
	private String errorMessage=null;
	private String vdsCode=null;
	private String carlineCode=null;
	private String wmiCode=null;
	
	private String errorCodes=null;
	/* InfoManager CategoryITO of the markets not yet moved to Kapture. Held as Object so that
	 * this class does not need the InfoManager library; those flows cast it back. */
	private Object categoryITO= null;
	private String remarks=null;
	/*
	 * TO BE USED FOR CHECKING IN DELETE AND PRINT MAPPED SOURCE PATHS IN REPORTS.
	 */
	private ArrayList<String> dependentSourcePaths = new ArrayList<String>();
	
	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public String getCarlineCode() {
		return carlineCode;
	}

	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
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

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getVdsCode() {
		return vdsCode;
	}

	public void setVdsCode(String vdsCode) {
		this.vdsCode = vdsCode;
	}

	public String getCategoryType() {
		return categoryType;
	}

	public void setCategoryType(String categoryType) {
		this.categoryType = categoryType;
	}

	public ArrayList<String> getDependentSourcePaths() {
		return dependentSourcePaths;
	}

	public void setDependentSourcePaths(ArrayList<String> dependentSourcePaths) {
		this.dependentSourcePaths = dependentSourcePaths;
	}

	public boolean isCategoryDeleted() {
		return categoryDeleted;
	}

	public void setCategoryDeleted(boolean categoryDeleted) {
		this.categoryDeleted = categoryDeleted;
	}

	public ContentDetails getContentDetails() {
		return contentDetails;
	}

	public void setContentDetails(ContentDetails contentDetails) {
		this.contentDetails = contentDetails;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryGuid() {
		return categoryGuid;
	}

	public void setCategoryGuid(String categoryGuid) {
		this.categoryGuid = categoryGuid;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getCategoryRefKey() {
		return categoryRefKey;
	}

	public void setCategoryRefKey(String categoryRefKey) {
		this.categoryRefKey = categoryRefKey;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public String getErrorCodes() {
		return errorCodes;
	}

	public void setErrorCodes(String errorCodes) {
		this.errorCodes = errorCodes;
	}

	public Object getCategoryITO() {
		return categoryITO;
	}

	public void setCategoryITO(Object categoryITO) {
		this.categoryITO = categoryITO;
	}

	/*
	 * KAPTURE - set when the category was found in Kapture (k_categories) for the document's
	 * locale. kaptureRefKey is the reference key exactly as Kapture stores it; a null value
	 * means the category does not exist there. Used by the MC flow in place of categoryITO.
	 */
	private String kaptureRefKey=null;
	private String kaptureName=null;

	public String getKaptureRefKey() {
		return kaptureRefKey;
	}

	public void setKaptureRefKey(String kaptureRefKey) {
		this.kaptureRefKey = kaptureRefKey;
	}

	public String getKaptureName() {
		return kaptureName;
	}

	public void setKaptureName(String kaptureName) {
		this.kaptureName = kaptureName;
	}
	
	
	
}
