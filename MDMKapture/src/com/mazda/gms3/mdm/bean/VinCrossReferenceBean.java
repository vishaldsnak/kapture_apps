package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.mdm.vo.VinCrossReferenceDetails;

public class VinCrossReferenceBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	private ArrayList<VinCrossReferenceDetails> crossReferenceList = new ArrayList<VinCrossReferenceDetails>();
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private VinCrossReferenceDetails fieldDetails = new VinCrossReferenceDetails();
	private String selectedRows=null;
	private String actionClicked=null;
	private String updatedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private String reportViewPath=null;
	
	private ArrayList<VinCrossReferenceDetails> crossReferenceListToImport=new ArrayList<VinCrossReferenceDetails>();
	
	
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	public ArrayList<VinCrossReferenceDetails> getCrossReferenceListToImport() {
		return crossReferenceListToImport;
	}
	public void setCrossReferenceListToImport(
			ArrayList<VinCrossReferenceDetails> crossReferenceListToImport) {
		this.crossReferenceListToImport = crossReferenceListToImport;
	}
	public String getInfoMessage() {
		return infoMessage;
	}
	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}
	public String getCountryLocaleId() {
		return countryLocaleId;
	}
	public void setCountryLocaleId(String countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
	}
	public ArrayList<CountryLocaleDetails> getCountryLocaleList() {
		return countryLocaleList;
	}
	public void setCountryLocaleList(
			ArrayList<CountryLocaleDetails> countryLocaleList) {
		this.countryLocaleList = countryLocaleList;
	}
	public String getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(String manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
	}
	public ArrayList<ManualLanguageDetails> getLanguageList() {
		return languageList;
	}
	public void setLanguageList(ArrayList<ManualLanguageDetails> languageList) {
		this.languageList = languageList;
	}
	public String getActionClicked() {
		return actionClicked;
	}
	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
	}
	public String getUpdatedRows() {
		return updatedRows;
	}
	public void setUpdatedRows(String updatedRows) {
		this.updatedRows = updatedRows;
	}
	public String getReportViewPath() {
		return reportViewPath;
	}
	public void setReportViewPath(String reportViewPath) {
		this.reportViewPath = reportViewPath;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getSuccessMessage() {
		return successMessage;
	}
	public void setSuccessMessage(String successMessage) {
		this.successMessage = successMessage;
	}
	public ArrayList<VinCrossReferenceDetails> getCrossReferenceList() {
		return crossReferenceList;
	}
	public void setCrossReferenceList(
			ArrayList<VinCrossReferenceDetails> crossReferenceList) {
		this.crossReferenceList = crossReferenceList;
	}
	public ArrayList<SelectItemDetails> getFlagList() {
		return flagList;
	}
	public void setFlagList(ArrayList<SelectItemDetails> flagList) {
		this.flagList = flagList;
	}
	public VinCrossReferenceDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(VinCrossReferenceDetails fieldDetails) {
		this.fieldDetails = fieldDetails;
	}
	public String getSelectedRows() {
		return selectedRows;
	}
	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
	}
	public boolean isShowUpdate() {
		return showUpdate;
	}
	public void setShowUpdate(boolean showUpdate) {
		this.showUpdate = showUpdate;
	}
	public String getDisplayPageNo() {
		return displayPageNo;
	}
	public void setDisplayPageNo(String displayPageNo) {
		this.displayPageNo = displayPageNo;
	}
	public String getDisplayPageLength() {
		return displayPageLength;
	}
	public void setDisplayPageLength(String displayPageLength) {
		this.displayPageLength = displayPageLength;
	}
	public boolean isShowReadControls() {
		return showReadControls;
	}
	public void setShowReadControls(boolean showReadControls) {
		this.showReadControls = showReadControls;
	}
	public boolean isShowWriteControls() {
		return showWriteControls;
	}
	public void setShowWriteControls(boolean showWriteControls) {
		this.showWriteControls = showWriteControls;
	}
}