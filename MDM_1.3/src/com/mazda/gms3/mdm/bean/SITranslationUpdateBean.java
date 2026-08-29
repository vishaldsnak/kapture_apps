package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SITranslationScheduleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class SITranslationUpdateBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage = null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	
	private String actionClicked = null;
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	private ArrayList<SelectItemDetails> operationTypeList=new ArrayList<SelectItemDetails>();
	private String operationTypeSelected=AccessManagementInterface.OPERATION_TYPE_EXPORT;
	
	private ArrayList<String> documentIdListToImport = new ArrayList<String>();
	private ArrayList<String> totalDocumentIdList = new ArrayList<String>();
	private ArrayList<String> documentIdList = new ArrayList<String>();
	private ArrayList<String> failedDocumentIdList = new ArrayList<String>();
	// max 5 documentIds
	private String documentIdInput=null;
	private boolean showExportBlock=false;
	private boolean showImportBlock=false;
	
	private ArrayList<SITranslationScheduleDetails> scheduleList = new ArrayList<SITranslationScheduleDetails>();
	private boolean reloadJSP=false;
	
	private String scheduleDisplayPageNo=null;
	private String scheduleDisplayPageLength = null;
	
	private String importFilesCount=null;
	
	public ArrayList<String> getTotalDocumentIdList() {
		return totalDocumentIdList;
	}
	public void setTotalDocumentIdList(ArrayList<String> totalDocumentIdList) {
		this.totalDocumentIdList = totalDocumentIdList;
	}
	public ArrayList<String> getFailedDocumentIdList() {
		return failedDocumentIdList;
	}
	public void setFailedDocumentIdList(ArrayList<String> failedDocumentIdList) {
		this.failedDocumentIdList = failedDocumentIdList;
	}
	public String getImportFilesCount() {
		return importFilesCount;
	}
	public void setImportFilesCount(String importFilesCount) {
		this.importFilesCount = importFilesCount;
	}
	public ArrayList<String> getDocumentIdListToImport() {
		return documentIdListToImport;
	}
	public void setDocumentIdListToImport(ArrayList<String> documentIdListToImport) {
		this.documentIdListToImport = documentIdListToImport;
	}
	public String getScheduleDisplayPageNo() {
		return scheduleDisplayPageNo;
	}
	public void setScheduleDisplayPageNo(String scheduleDisplayPageNo) {
		this.scheduleDisplayPageNo = scheduleDisplayPageNo;
	}
	public String getScheduleDisplayPageLength() {
		return scheduleDisplayPageLength;
	}
	public void setScheduleDisplayPageLength(String scheduleDisplayPageLength) {
		this.scheduleDisplayPageLength = scheduleDisplayPageLength;
	}
	public boolean isReloadJSP() {
		return reloadJSP;
	}
	public void setReloadJSP(boolean reloadJSP) {
		this.reloadJSP = reloadJSP;
	}
	public ArrayList<SITranslationScheduleDetails> getScheduleList() {
		return scheduleList;
	}
	public void setScheduleList(ArrayList<SITranslationScheduleDetails> scheduleList) {
		this.scheduleList = scheduleList;
	}
	public boolean isShowExportBlock() {
		return showExportBlock;
	}
	public void setShowExportBlock(boolean showExportBlock) {
		this.showExportBlock = showExportBlock;
	}
	public boolean isShowImportBlock() {
		return showImportBlock;
	}
	public void setShowImportBlock(boolean showImportBlock) {
		this.showImportBlock = showImportBlock;
	}
	public ArrayList<String> getDocumentIdList() {
		return documentIdList;
	}
	public void setDocumentIdList(ArrayList<String> documentIdList) {
		this.documentIdList = documentIdList;
	}
	public String getDocumentIdInput() {
		return documentIdInput;
	}
	public void setDocumentIdInput(String documentIdInput) {
		this.documentIdInput = documentIdInput;
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
	public ArrayList<SelectItemDetails> getOperationTypeList() {
		return operationTypeList;
	}
	public void setOperationTypeList(ArrayList<SelectItemDetails> operationTypeList) {
		this.operationTypeList = operationTypeList;
	}
	public String getOperationTypeSelected() {
		return operationTypeSelected;
	}
	public void setOperationTypeSelected(String operationTypeSelected) {
		this.operationTypeSelected = operationTypeSelected;
	}
}