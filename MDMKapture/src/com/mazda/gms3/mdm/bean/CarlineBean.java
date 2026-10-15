package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class CarlineBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage = null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private ArrayList<SelectItemDetails> esiCategoryFlagList = new ArrayList<SelectItemDetails>();
	private ArrayList<SelectItemDetails> modelTypeList = new ArrayList<SelectItemDetails>();
	private CarlineDetails fieldDetails = new CarlineDetails();
	private String selectedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private ArrayList<CarlineDetails> carList = new ArrayList<CarlineDetails>();
	private ArrayList<CarlineDetails> allDataCarList = new ArrayList<CarlineDetails>();
	private ArrayList<CarlineDetails> carListToImport = new ArrayList<CarlineDetails>();
	
	/*
	 * YEAR START - YEAR END IS ALWAYS MANDATORY
	 * DATE CHANGE = 23 JUNE 2018
	 */
//	private boolean yearStartMandatory = false;
	
	private String updatedRows = null;
	private String actionClicked = null;
	private String reportViewPath = null;
	
	private boolean showButtons=false;
	private boolean showView=false;
	private boolean showAdd=false;
	private boolean mnaoContent=false;
	
	private String searchWmiId=null;
	private String searchCarlineNameId=null;
	private String searchCarlineCodeId=null;
	private String searchESICategoryFlagId=null;
	private String searchModelTypeId=null;
	private ArrayList<CarlineDetails> searchWmiList =new ArrayList<CarlineDetails>();
	private ArrayList<CarlineDetails> searchCarlineNameList = new ArrayList<CarlineDetails>();
	private ArrayList<CarlineDetails> searchCarlineCodeList = new ArrayList<CarlineDetails>();
	
	private String addNewWmiId=null;
	private String addNewCarlineNameId=null;
	private String addNewESICategoryFlagId=null;
	private String addNewModelTypeId=null;
	private ArrayList<CarlineDetails> addNewWmiList =new ArrayList<CarlineDetails>();
	private ArrayList<CarlineDetails> addNewCarlineNameList = new ArrayList<CarlineDetails>();
	
	private boolean showAddNewWMIField = false;
	private boolean showAddNewCarlineNameField = false;
	private String newWmiFieldValue=null;
	private String newCarlineNameEngFieldValue = null;
	private String newCarlineNameRegFieldValue = null;
	
	
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
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
	public String getInfoMessage() {
		return infoMessage;
	}
	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}
	public String getNewWmiFieldValue() {
		return newWmiFieldValue;
	}
	public void setNewWmiFieldValue(String newWmiFieldValue) {
		this.newWmiFieldValue = newWmiFieldValue;
	}
	public String getNewCarlineNameEngFieldValue() {
		return newCarlineNameEngFieldValue;
	}
	public void setNewCarlineNameEngFieldValue(String newCarlineNameEngFieldValue) {
		this.newCarlineNameEngFieldValue = newCarlineNameEngFieldValue;
	}
	public String getNewCarlineNameRegFieldValue() {
		return newCarlineNameRegFieldValue;
	}
	public void setNewCarlineNameRegFieldValue(String newCarlineNameRegFieldValue) {
		this.newCarlineNameRegFieldValue = newCarlineNameRegFieldValue;
	}
	public boolean isShowAddNewWMIField() {
		return showAddNewWMIField;
	}
	public void setShowAddNewWMIField(boolean showAddNewWMIField) {
		this.showAddNewWMIField = showAddNewWMIField;
	}
	public boolean isShowAddNewCarlineNameField() {
		return showAddNewCarlineNameField;
	}
	public void setShowAddNewCarlineNameField(boolean showAddNewCarlineNameField) {
		this.showAddNewCarlineNameField = showAddNewCarlineNameField;
	}
	public ArrayList<CarlineDetails> getAllDataCarList() {
		return allDataCarList;
	}
	public void setAllDataCarList(ArrayList<CarlineDetails> allDataCarList) {
		this.allDataCarList = allDataCarList;
	}
	public ArrayList<CarlineDetails> getCarListToImport() {
		return carListToImport;
	}
	public void setCarListToImport(ArrayList<CarlineDetails> carListToImport) {
		this.carListToImport = carListToImport;
	}
	public String getUpdatedRows() {
		return updatedRows;
	}
	public void setUpdatedRows(String updatedRows) {
		this.updatedRows = updatedRows;
	}
	public String getActionClicked() {
		return actionClicked;
	}
	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
	}
	public String getReportViewPath() {
		return reportViewPath;
	}
	public void setReportViewPath(String reportViewPath) {
		this.reportViewPath = reportViewPath;
	}
	public ArrayList<SelectItemDetails> getEsiCategoryFlagList() {
		return esiCategoryFlagList;
	}
	public void setEsiCategoryFlagList(
			ArrayList<SelectItemDetails> esiCategoryFlagList) {
		this.esiCategoryFlagList = esiCategoryFlagList;
	}
	public ArrayList<SelectItemDetails> getModelTypeList() {
		return modelTypeList;
	}
	public void setModelTypeList(ArrayList<SelectItemDetails> modelTypeList) {
		this.modelTypeList = modelTypeList;
	}
	public boolean isShowButtons() {
		return showButtons;
	}
	public void setShowButtons(boolean showButtons) {
		this.showButtons = showButtons;
	}
	public boolean isShowView() {
		return showView;
	}
	public void setShowView(boolean showView) {
		this.showView = showView;
	}
	public boolean isShowAdd() {
		return showAdd;
	}
	public void setShowAdd(boolean showAdd) {
		this.showAdd = showAdd;
	}
	public boolean isMnaoContent() {
		return mnaoContent;
	}
	public void setMnaoContent(boolean mnaoContent) {
		this.mnaoContent = mnaoContent;
	}
	public String getSearchWmiId() {
		return searchWmiId;
	}
	public void setSearchWmiId(String searchWmiId) {
		this.searchWmiId = searchWmiId;
	}
	public String getSearchCarlineNameId() {
		return searchCarlineNameId;
	}
	public void setSearchCarlineNameId(String searchCarlineNameId) {
		this.searchCarlineNameId = searchCarlineNameId;
	}
	public String getSearchCarlineCodeId() {
		return searchCarlineCodeId;
	}
	public void setSearchCarlineCodeId(String searchCarlineCodeId) {
		this.searchCarlineCodeId = searchCarlineCodeId;
	}
	public String getSearchESICategoryFlagId() {
		return searchESICategoryFlagId;
	}
	public void setSearchESICategoryFlagId(String searchESICategoryFlagId) {
		this.searchESICategoryFlagId = searchESICategoryFlagId;
	}
	public String getSearchModelTypeId() {
		return searchModelTypeId;
	}
	public void setSearchModelTypeId(String searchModelTypeId) {
		this.searchModelTypeId = searchModelTypeId;
	}
	public ArrayList<CarlineDetails> getSearchWmiList() {
		return searchWmiList;
	}
	public void setSearchWmiList(ArrayList<CarlineDetails> searchWmiList) {
		this.searchWmiList = searchWmiList;
	}
	public ArrayList<CarlineDetails> getSearchCarlineNameList() {
		return searchCarlineNameList;
	}
	public void setSearchCarlineNameList(
			ArrayList<CarlineDetails> searchCarlineNameList) {
		this.searchCarlineNameList = searchCarlineNameList;
	}
	public ArrayList<CarlineDetails> getSearchCarlineCodeList() {
		return searchCarlineCodeList;
	}
	public void setSearchCarlineCodeList(
			ArrayList<CarlineDetails> searchCarlineCodeList) {
		this.searchCarlineCodeList = searchCarlineCodeList;
	}
	public String getAddNewWmiId() {
		return addNewWmiId;
	}
	public void setAddNewWmiId(String addNewWmiId) {
		this.addNewWmiId = addNewWmiId;
	}
	public String getAddNewCarlineNameId() {
		return addNewCarlineNameId;
	}
	public void setAddNewCarlineNameId(String addNewCarlineNameId) {
		this.addNewCarlineNameId = addNewCarlineNameId;
	}
	public String getAddNewESICategoryFlagId() {
		return addNewESICategoryFlagId;
	}
	public void setAddNewESICategoryFlagId(String addNewESICategoryFlagId) {
		this.addNewESICategoryFlagId = addNewESICategoryFlagId;
	}
	public String getAddNewModelTypeId() {
		return addNewModelTypeId;
	}
	public void setAddNewModelTypeId(String addNewModelTypeId) {
		this.addNewModelTypeId = addNewModelTypeId;
	}
	public ArrayList<CarlineDetails> getAddNewWmiList() {
		return addNewWmiList;
	}
	public void setAddNewWmiList(ArrayList<CarlineDetails> addNewWmiList) {
		this.addNewWmiList = addNewWmiList;
	}
	public ArrayList<CarlineDetails> getAddNewCarlineNameList() {
		return addNewCarlineNameList;
	}
	public void setAddNewCarlineNameList(
			ArrayList<CarlineDetails> addNewCarlineNameList) {
		this.addNewCarlineNameList = addNewCarlineNameList;
	}
//	public boolean isYearStartMandatory() {
//		return yearStartMandatory;
//	}
//	public void setYearStartMandatory(boolean yearStartMandatory) {
//		this.yearStartMandatory = yearStartMandatory;
//	}
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
	public ArrayList<CarlineDetails> getCarList() {
		return carList;
	}
	public void setCarList(ArrayList<CarlineDetails> carList) {
		this.carList = carList;
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
	public ArrayList<ManualLanguageDetails> getLanguageList() {
		return languageList;
	}
	public void setLanguageList(ArrayList<ManualLanguageDetails> languageList) {
		this.languageList = languageList;
	}
	public ArrayList<SelectItemDetails> getFlagList() {
		return flagList;
	}
	public void setFlagList(ArrayList<SelectItemDetails> flagList) {
		this.flagList = flagList;
	}
	public CarlineDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(CarlineDetails fieldDetails) {
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
}
