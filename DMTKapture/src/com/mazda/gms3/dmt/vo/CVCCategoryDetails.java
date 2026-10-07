package com.mazda.gms3.dmt.vo;

public class CVCCategoryDetails {

	private String categoryCode=null;
	private String subCategoryCode=null;
	private String symptomCode=null;
	private String subSymptomCode=null;
	private String conditionCode=null;
	private String cvcRefKey=null;
	
	
	public String getCvcRefKey() {
		return cvcRefKey;
	}
	public void setCvcRefKey(String cvcRefKey) {
		this.cvcRefKey = cvcRefKey;
	}
	public String getSubCategoryCode() {
		return subCategoryCode;
	}
	public void setSubCategoryCode(String subCategoryCode) {
		this.subCategoryCode = subCategoryCode;
	}
	public String getCategoryCode() {
		return categoryCode;
	}
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	public String getSymptomCode() {
		return symptomCode;
	}
	public void setSymptomCode(String symptomCode) {
		this.symptomCode = symptomCode;
	}
	public String getSubSymptomCode() {
		return subSymptomCode;
	}
	public void setSubSymptomCode(String subSymptomCode) {
		this.subSymptomCode = subSymptomCode;
	}
	public String getConditionCode() {
		return conditionCode;
	}
	public void setConditionCode(String conditionCode) {
		this.conditionCode = conditionCode;
	}
	
	
}
