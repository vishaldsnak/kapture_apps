package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.mdm.vo.VinDetails;

public class VinBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	private String carlineId=null;
	private String carlineNameEng=null;
//	private String carlineNameReg=null;
	private String carlineCode=null;
	private ArrayList<CarlineDetails> carlineList = new ArrayList<CarlineDetails>();
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private VinDetails fieldDetails = new VinDetails();
	private String selectedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private ArrayList<VinDetails> vinList = new ArrayList<VinDetails>();
	private String updatedRows = null;
	private String actionClicked = null;
	
	private ArrayList<VinDetails> vinListToImport = new ArrayList<VinDetails>();
	private String reportViewPath = null;
	
	private String searchGroupId=null;
	private String searchWMICode = null;
	private String searchVDSCode=null;
	private String searchVISStartRange=null;
	private String searchVISEndRange = null;
	
	
	private String addGroupId=null;
	private String addWmiCode=null;
	private String addVDSCode=null;
	private String addVISStartRange=null;
	private String addVISEndRange = null;
	
	private String newGroupCode=null;
	private String newVDSCode=null;
	private String newVISStartRange=null;
	private String newVISEndRange=null;
	
	private ArrayList<SelectItemDetails> searchWMIList = new ArrayList<SelectItemDetails>();
	private ArrayList<VinDetails> searchGroupList = new ArrayList<VinDetails>();
	private ArrayList<VinDetails> searchVDSList = new ArrayList<VinDetails>();
	private ArrayList<VinDetails> searchVISStartList = new ArrayList<VinDetails>();
	private ArrayList<VinDetails> searchVISEndList = new ArrayList<VinDetails>();
	
	private ArrayList<SelectItemDetails> addWMIList = new ArrayList<SelectItemDetails>();
	private ArrayList<VinDetails> addGroupList = new ArrayList<VinDetails>();
	private ArrayList<VinDetails> addVDSList = new ArrayList<VinDetails>();
	private ArrayList<VinDetails> addVISStartList = new ArrayList<VinDetails>();
	private ArrayList<VinDetails> addVISEndList = new ArrayList<VinDetails>();
	
	private ArrayList<VinDetails> allVINDataList = new ArrayList<VinDetails>();
	
	private boolean showButtons=false;
	private boolean showView=false;
	private boolean showAdd=false;
	
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
	
//	public String getCarlineNameReg() {
//		return carlineNameReg;
//	}
//	public void setCarlineNameReg(String carlineNameReg) {
//		this.carlineNameReg = carlineNameReg;
//	}
	public String getCarlineNameEng() {
		return carlineNameEng;
	}
	public void setCarlineNameEng(String carlineNameEng) {
		this.carlineNameEng = carlineNameEng;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getSearchGroupId() {
		return searchGroupId;
	}
	public void setSearchGroupId(String searchGroupId) {
		this.searchGroupId = searchGroupId;
	}
	public String getSearchWMICode() {
		return searchWMICode;
	}
	public void setSearchWMICode(String searchWMICode) {
		this.searchWMICode = searchWMICode;
	}
	public String getSearchVDSCode() {
		return searchVDSCode;
	}
	public void setSearchVDSCode(String searchVDSCode) {
		this.searchVDSCode = searchVDSCode;
	}
	public String getSearchVISStartRange() {
		return searchVISStartRange;
	}
	public void setSearchVISStartRange(String searchVISStartRange) {
		this.searchVISStartRange = searchVISStartRange;
	}
	public String getSearchVISEndRange() {
		return searchVISEndRange;
	}
	public void setSearchVISEndRange(String searchVISEndRange) {
		this.searchVISEndRange = searchVISEndRange;
	}
	public String getAddGroupId() {
		return addGroupId;
	}
	public void setAddGroupId(String addGroupId) {
		this.addGroupId = addGroupId;
	}
	public String getAddVDSCode() {
		return addVDSCode;
	}
	public void setAddVDSCode(String addVDSCode) {
		this.addVDSCode = addVDSCode;
	}
	public String getAddVISStartRange() {
		return addVISStartRange;
	}
	public void setAddVISStartRange(String addVISStartRange) {
		this.addVISStartRange = addVISStartRange;
	}
	public String getAddVISEndRange() {
		return addVISEndRange;
	}
	public void setAddVISEndRange(String addVISEndRange) {
		this.addVISEndRange = addVISEndRange;
	}
	public String getNewGroupCode() {
		return newGroupCode;
	}
	public void setNewGroupCode(String newGroupCode) {
		this.newGroupCode = newGroupCode;
	}
	public String getNewVDSCode() {
		return newVDSCode;
	}
	public void setNewVDSCode(String newVDSCode) {
		this.newVDSCode = newVDSCode;
	}
	public String getNewVISStartRange() {
		return newVISStartRange;
	}
	public void setNewVISStartRange(String newVISStartRange) {
		this.newVISStartRange = newVISStartRange;
	}
	public String getNewVISEndRange() {
		return newVISEndRange;
	}
	public void setNewVISEndRange(String newVISEndRange) {
		this.newVISEndRange = newVISEndRange;
	}
	public ArrayList<VinDetails> getSearchGroupList() {
		return searchGroupList;
	}
	public void setSearchGroupList(ArrayList<VinDetails> searchGroupList) {
		this.searchGroupList = searchGroupList;
	}
	public ArrayList<VinDetails> getSearchVDSList() {
		return searchVDSList;
	}
	public void setSearchVDSList(ArrayList<VinDetails> searchVDSList) {
		this.searchVDSList = searchVDSList;
	}
	public ArrayList<VinDetails> getSearchVISStartList() {
		return searchVISStartList;
	}
	public void setSearchVISStartList(ArrayList<VinDetails> searchVISStartList) {
		this.searchVISStartList = searchVISStartList;
	}
	public ArrayList<VinDetails> getSearchVISEndList() {
		return searchVISEndList;
	}
	public void setSearchVISEndList(ArrayList<VinDetails> searchVISEndList) {
		this.searchVISEndList = searchVISEndList;
	}
	public ArrayList<VinDetails> getAddGroupList() {
		return addGroupList;
	}
	public void setAddGroupList(ArrayList<VinDetails> addGroupList) {
		this.addGroupList = addGroupList;
	}
	public ArrayList<VinDetails> getAddVDSList() {
		return addVDSList;
	}
	public void setAddVDSList(ArrayList<VinDetails> addVDSList) {
		this.addVDSList = addVDSList;
	}
	public ArrayList<VinDetails> getAddVISStartList() {
		return addVISStartList;
	}
	public void setAddVISStartList(ArrayList<VinDetails> addVISStartList) {
		this.addVISStartList = addVISStartList;
	}
	public ArrayList<VinDetails> getAddVISEndList() {
		return addVISEndList;
	}
	public void setAddVISEndList(ArrayList<VinDetails> addVISEndList) {
		this.addVISEndList = addVISEndList;
	}
	public ArrayList<VinDetails> getAllVINDataList() {
		return allVINDataList;
	}
	public void setAllVINDataList(ArrayList<VinDetails> allVINDataList) {
		this.allVINDataList = allVINDataList;
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
	public String getInfoMessage() {
		return infoMessage;
	}
	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}
	public String getReportViewPath() {
		return reportViewPath;
	}
	public void setReportViewPath(String reportViewPath) {
		this.reportViewPath = reportViewPath;
	}
	public ArrayList<VinDetails> getVinListToImport() {
		return vinListToImport;
	}
	public void setVinListToImport(ArrayList<VinDetails> vinListToImport) {
		this.vinListToImport = vinListToImport;
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
	public String getCarlineId() {
		return carlineId;
	}
	public void setCarlineId(String carlineId) {
		this.carlineId = carlineId;
	}
	public ArrayList<CarlineDetails> getCarlineList() {
		return carlineList;
	}
	public void setCarlineList(ArrayList<CarlineDetails> carlineList) {
		this.carlineList = carlineList;
	}
	public ArrayList<VinDetails> getVinList() {
		return vinList;
	}
	public void setVinList(ArrayList<VinDetails> vinList) {
		this.vinList = vinList;
	}
	public String getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(String manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
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
	public VinDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(VinDetails fieldDetails) {
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
	public String getAddWmiCode() {
		return addWmiCode;
	}
	public void setAddWmiCode(String addWmiCode) {
		this.addWmiCode = addWmiCode;
	}
	public ArrayList<SelectItemDetails> getSearchWMIList() {
		return searchWMIList;
	}
	public void setSearchWMIList(ArrayList<SelectItemDetails> searchWMIList) {
		this.searchWMIList = searchWMIList;
	}
	public ArrayList<SelectItemDetails> getAddWMIList() {
		return addWMIList;
	}
	public void setAddWMIList(ArrayList<SelectItemDetails> addWMIList) {
		this.addWMIList = addWMIList;
	}
	
	
}
