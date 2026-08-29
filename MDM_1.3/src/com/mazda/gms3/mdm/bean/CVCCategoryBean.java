package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.CVCCategoryDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;


public class CVCCategoryBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private CVCCategoryDetails fieldDetails = new CVCCategoryDetails();
	private String selectedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private ArrayList<CVCCategoryDetails> categoryList = new ArrayList<CVCCategoryDetails>();
	private String actionClicked=null;
	private String updatedRows=null;
	private ArrayList<CVCCategoryDetails> categoryListToImport=new ArrayList<CVCCategoryDetails>();
	private String reportViewPath=null;
	
	private ArrayList<CVCCategoryDetails> allCategoryDataList = new ArrayList<CVCCategoryDetails>();
	private boolean showButtons=false;
	private boolean showView=false;
	private boolean showAdd=false;
	
	private String searchCatLevel1Code=null;
	private String searchCatLevel2Code=null;
	private String searchCatLevel3Code=null;
	private String searchCatLevel4Code=null;
	
	private String addCatLevel1Code=null;
	private String addCatLevel2Code=null;
	private String addCatLevel3Code=null;
	private String addCatLevel4Code=null;
	
	private String newCatLevel1Code=null;
	private String newCatLevel1Name=null;
	
	private String newCatLevel2Code=null;
	private String newCatLevel2Name=null;
	
	private String newCatLevel3Code=null;
	private String newCatLevel3Name=null;
	
	private String newCatLevel4Code=null;
	private String newCatLevel4Name=null;
	
	
	private ArrayList<CVCCategoryDetails> searchCatLevel1List = new ArrayList<CVCCategoryDetails>();
	private ArrayList<CVCCategoryDetails> searchCatLevel2List = new ArrayList<CVCCategoryDetails>();
	private ArrayList<CVCCategoryDetails> searchCatLevel3List = new ArrayList<CVCCategoryDetails>();
	private ArrayList<CVCCategoryDetails> searchCatLevel4List = new ArrayList<CVCCategoryDetails>();
	
	
	private ArrayList<CVCCategoryDetails> addCatLevel1List = new ArrayList<CVCCategoryDetails>();
	private ArrayList<CVCCategoryDetails> addCatLevel2List = new ArrayList<CVCCategoryDetails>();
	private ArrayList<CVCCategoryDetails> addCatLevel3List = new ArrayList<CVCCategoryDetails>();
	private ArrayList<CVCCategoryDetails> addCatLevel4List = new ArrayList<CVCCategoryDetails>();
	
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	
	
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
	public String getSearchCatLevel3Code() {
		return searchCatLevel3Code;
	}
	public void setSearchCatLevel3Code(String searchCatLevel3Code) {
		this.searchCatLevel3Code = searchCatLevel3Code;
	}
	public String getSearchCatLevel4Code() {
		return searchCatLevel4Code;
	}
	public void setSearchCatLevel4Code(String searchCatLevel4Code) {
		this.searchCatLevel4Code = searchCatLevel4Code;
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
	public String getAddCatLevel3Code() {
		return addCatLevel3Code;
	}
	public void setAddCatLevel3Code(String addCatLevel3Code) {
		this.addCatLevel3Code = addCatLevel3Code;
	}
	public String getAddCatLevel4Code() {
		return addCatLevel4Code;
	}
	public void setAddCatLevel4Code(String addCatLevel4Code) {
		this.addCatLevel4Code = addCatLevel4Code;
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
	public String getNewCatLevel3Code() {
		return newCatLevel3Code;
	}
	public void setNewCatLevel3Code(String newCatLevel3Code) {
		this.newCatLevel3Code = newCatLevel3Code;
	}
	public String getNewCatLevel3Name() {
		return newCatLevel3Name;
	}
	public void setNewCatLevel3Name(String newCatLevel3Name) {
		this.newCatLevel3Name = newCatLevel3Name;
	}
	public String getNewCatLevel4Code() {
		return newCatLevel4Code;
	}
	public void setNewCatLevel4Code(String newCatLevel4Code) {
		this.newCatLevel4Code = newCatLevel4Code;
	}
	public String getNewCatLevel4Name() {
		return newCatLevel4Name;
	}
	public void setNewCatLevel4Name(String newCatLevel4Name) {
		this.newCatLevel4Name = newCatLevel4Name;
	}
	public ArrayList<CVCCategoryDetails> getSearchCatLevel1List() {
		return searchCatLevel1List;
	}
	public void setSearchCatLevel1List(
			ArrayList<CVCCategoryDetails> searchCatLevel1List) {
		this.searchCatLevel1List = searchCatLevel1List;
	}
	public ArrayList<CVCCategoryDetails> getSearchCatLevel2List() {
		return searchCatLevel2List;
	}
	public void setSearchCatLevel2List(
			ArrayList<CVCCategoryDetails> searchCatLevel2List) {
		this.searchCatLevel2List = searchCatLevel2List;
	}
	public ArrayList<CVCCategoryDetails> getSearchCatLevel3List() {
		return searchCatLevel3List;
	}
	public void setSearchCatLevel3List(
			ArrayList<CVCCategoryDetails> searchCatLevel3List) {
		this.searchCatLevel3List = searchCatLevel3List;
	}
	public ArrayList<CVCCategoryDetails> getSearchCatLevel4List() {
		return searchCatLevel4List;
	}
	public void setSearchCatLevel4List(
			ArrayList<CVCCategoryDetails> searchCatLevel4List) {
		this.searchCatLevel4List = searchCatLevel4List;
	}
	public ArrayList<CVCCategoryDetails> getAddCatLevel1List() {
		return addCatLevel1List;
	}
	public void setAddCatLevel1List(ArrayList<CVCCategoryDetails> addCatLevel1List) {
		this.addCatLevel1List = addCatLevel1List;
	}
	public ArrayList<CVCCategoryDetails> getAddCatLevel2List() {
		return addCatLevel2List;
	}
	public void setAddCatLevel2List(ArrayList<CVCCategoryDetails> addCatLevel2List) {
		this.addCatLevel2List = addCatLevel2List;
	}
	public ArrayList<CVCCategoryDetails> getAddCatLevel3List() {
		return addCatLevel3List;
	}
	public void setAddCatLevel3List(ArrayList<CVCCategoryDetails> addCatLevel3List) {
		this.addCatLevel3List = addCatLevel3List;
	}
	public ArrayList<CVCCategoryDetails> getAddCatLevel4List() {
		return addCatLevel4List;
	}
	public void setAddCatLevel4List(ArrayList<CVCCategoryDetails> addCatLevel4List) {
		this.addCatLevel4List = addCatLevel4List;
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
	public ArrayList<CVCCategoryDetails> getCategoryListToImport() {
		return categoryListToImport;
	}
	public void setCategoryListToImport(
			ArrayList<CVCCategoryDetails> categoryListToImport) {
		this.categoryListToImport = categoryListToImport;
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
	public CVCCategoryDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(CVCCategoryDetails fieldDetails) {
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
	public ArrayList<CVCCategoryDetails> getCategoryList() {
		return categoryList;
	}
	public void setCategoryList(ArrayList<CVCCategoryDetails> categoryList) {
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
	public ArrayList<CVCCategoryDetails> getAllCategoryDataList() {
		return allCategoryDataList;
	}
	public void setAllCategoryDataList(
			ArrayList<CVCCategoryDetails> allCategoryDataList) {
		this.allCategoryDataList = allCategoryDataList;
	}
}