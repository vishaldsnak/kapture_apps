package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.CategoryDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class CategoryBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private CategoryDetails fieldDetails = new CategoryDetails();
	private String selectedRows=null;
	private String actionClicked=null;
	private String updatedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private ArrayList<CategoryDetails> categoryList = new ArrayList<CategoryDetails>();
	
	private ArrayList<CategoryDetails> categoryListToImport=new ArrayList<CategoryDetails>();
	private String reportViewPath=null;
	
	
	private ArrayList<CategoryDetails> allCategoryDataList = new ArrayList<CategoryDetails>();
	private boolean showButtons=false;
	private boolean showView=false;
	private boolean showAdd=false;
	
	private String searchCatLevel1Code=null;
	private String searchCatLevel2Code=null;
	
	private String addCatLevel1Code=null;
	private String addCatLevel2Code=null;
	
	private String newCatLevel1Code=null;
	private String newCatLevel1Name=null;
	
	private String newCatLevel2Code=null;
	private String newCatLevel2Name=null;
	
	private ArrayList<CategoryDetails> searchCatLevel1List = new ArrayList<CategoryDetails>();
	private ArrayList<CategoryDetails> searchCatLevel2List = new ArrayList<CategoryDetails>();
	
	private ArrayList<CategoryDetails> addCatLevel1List = new ArrayList<CategoryDetails>();
	private ArrayList<CategoryDetails> addCatLevel2List = new ArrayList<CategoryDetails>();
	
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
	
	public ArrayList<CategoryDetails> getAllCategoryDataList() {
		return allCategoryDataList;
	}
	public void setAllCategoryDataList(
			ArrayList<CategoryDetails> allCategoryDataList) {
		this.allCategoryDataList = allCategoryDataList;
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
	public String getSearchCatLevel1Code() {
		return searchCatLevel1Code;
	}
	public void setSearchCatLevel1Code(String searchCatLevel1Code) {
		this.searchCatLevel1Code = searchCatLevel1Code;
	}
	public String getSearchCatLevel2Code() {
		return searchCatLevel2Code;
	}
	public void setSearchCatLevel2Code(String searchCatLevel2Code) {
		this.searchCatLevel2Code = searchCatLevel2Code;
	}
	public String getAddCatLevel1Code() {
		return addCatLevel1Code;
	}
	public void setAddCatLevel1Code(String addCatLevel1Code) {
		this.addCatLevel1Code = addCatLevel1Code;
	}
	public String getAddCatLevel2Code() {
		return addCatLevel2Code;
	}
	public void setAddCatLevel2Code(String addCatLevel2Code) {
		this.addCatLevel2Code = addCatLevel2Code;
	}
	public String getNewCatLevel1Code() {
		return newCatLevel1Code;
	}
	public void setNewCatLevel1Code(String newCatLevel1Code) {
		this.newCatLevel1Code = newCatLevel1Code;
	}
	public String getNewCatLevel1Name() {
		return newCatLevel1Name;
	}
	public void setNewCatLevel1Name(String newCatLevel1Name) {
		this.newCatLevel1Name = newCatLevel1Name;
	}
	public String getNewCatLevel2Code() {
		return newCatLevel2Code;
	}
	public void setNewCatLevel2Code(String newCatLevel2Code) {
		this.newCatLevel2Code = newCatLevel2Code;
	}
	public String getNewCatLevel2Name() {
		return newCatLevel2Name;
	}
	public void setNewCatLevel2Name(String newCatLevel2Name) {
		this.newCatLevel2Name = newCatLevel2Name;
	}
	public ArrayList<CategoryDetails> getSearchCatLevel1List() {
		return searchCatLevel1List;
	}
	public void setSearchCatLevel1List(
			ArrayList<CategoryDetails> searchCatLevel1List) {
		this.searchCatLevel1List = searchCatLevel1List;
	}
	public ArrayList<CategoryDetails> getSearchCatLevel2List() {
		return searchCatLevel2List;
	}
	public void setSearchCatLevel2List(
			ArrayList<CategoryDetails> searchCatLevel2List) {
		this.searchCatLevel2List = searchCatLevel2List;
	}
	public ArrayList<CategoryDetails> getAddCatLevel1List() {
		return addCatLevel1List;
	}
	public void setAddCatLevel1List(ArrayList<CategoryDetails> addCatLevel1List) {
		this.addCatLevel1List = addCatLevel1List;
	}
	public ArrayList<CategoryDetails> getAddCatLevel2List() {
		return addCatLevel2List;
	}
	public void setAddCatLevel2List(ArrayList<CategoryDetails> addCatLevel2List) {
		this.addCatLevel2List = addCatLevel2List;
	}
	public ArrayList<CategoryDetails> getCategoryListToImport() {
		return categoryListToImport;
	}
	public void setCategoryListToImport(
			ArrayList<CategoryDetails> categoryListToImport) {
		this.categoryListToImport = categoryListToImport;
	}
	public String getReportViewPath() {
		return reportViewPath;
	}
	public void setReportViewPath(String reportViewPath) {
		this.reportViewPath = reportViewPath;
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
	public ArrayList<SelectItemDetails> getFlagList() {
		return flagList;
	}
	public void setFlagList(ArrayList<SelectItemDetails> flagList) {
		this.flagList = flagList;
	}
	public CategoryDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(CategoryDetails fieldDetails) {
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
	public ArrayList<CategoryDetails> getCategoryList() {
		return categoryList;
	}
	public void setCategoryList(ArrayList<CategoryDetails> categoryList) {
		this.categoryList = categoryList;
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
	public String getInfoMessage() {
		return infoMessage;
	}
	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}
}