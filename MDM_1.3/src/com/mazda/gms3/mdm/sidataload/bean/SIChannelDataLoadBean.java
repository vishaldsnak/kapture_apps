package com.mazda.gms3.mdm.sidataload.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.sidataload.vo.SIChannelScheduleDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class SIChannelDataLoadBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList =null;
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList =null;
	private ArrayList<SelectItemDetails> operationTypeList =null;
	private String selectedOperationType=null;
	
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	private String oldDocumentId=null;
	private String actionClicked=null;
	private boolean showOldDocumentIdField=false;
	
	private ArrayList<SIChannelScheduleDetails> scheduleList = null;
	private boolean reloadJSP=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	

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
	public boolean isReloadJSP() {
		return reloadJSP;
	}
	public void setReloadJSP(boolean reloadJSP) {
		this.reloadJSP = reloadJSP;
	}
	public ArrayList<SIChannelScheduleDetails> getScheduleList() {
		return scheduleList;
	}
	public void setScheduleList(ArrayList<SIChannelScheduleDetails> scheduleList) {
		this.scheduleList = scheduleList;
	}
	public String getOldDocumentId() {
		return oldDocumentId;
	}
	public void setOldDocumentId(String oldDocumentId) {
		this.oldDocumentId = oldDocumentId;
	}
	public boolean isShowOldDocumentIdField() {
		return showOldDocumentIdField;
	}
	public void setShowOldDocumentIdField(boolean showOldDocumentIdField) {
		this.showOldDocumentIdField = showOldDocumentIdField;
	}
	public String getActionClicked() {
		return actionClicked;
	}
	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
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
	public void setCountryLocaleList(ArrayList<CountryLocaleDetails> countryLocaleList) {
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
	public ArrayList<SelectItemDetails> getOperationTypeList() {
		return operationTypeList;
	}
	public void setOperationTypeList(ArrayList<SelectItemDetails> operationTypeList) {
		this.operationTypeList = operationTypeList;
	}
	public String getSelectedOperationType() {
		return selectedOperationType;
	}
	public void setSelectedOperationType(String selectedOperationType) {
		this.selectedOperationType = selectedOperationType;
	}
	
	
	
}
