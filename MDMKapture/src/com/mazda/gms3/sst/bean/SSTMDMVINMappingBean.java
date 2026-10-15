package com.mazda.gms3.sst.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.vo.SSTMDMVinDetails;


public class SSTMDMVINMappingBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	private ArrayList<SSTMDMVinDetails> mappingList = new ArrayList<SSTMDMVinDetails>();
	
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private SSTMDMVinDetails fieldDetails = new SSTMDMVinDetails();
	private String selectedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	
	private String updatedRows = null;
	private String actionClicked = null;
	
	private ArrayList<SSTMDMVinDetails> mappingListToImport = new ArrayList<SSTMDMVinDetails>();
	private String reportViewPath = null;
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	private ArrayList<CarlineDetails> carlineCodeList = new ArrayList<CarlineDetails>();
	private ArrayList<SelectItemDetails> wmiCodeList = new ArrayList<SelectItemDetails>();
	private ArrayList<SelectItemDetails> vdsCodeList = new ArrayList<SelectItemDetails>();
	private ArrayList<SelectItemDetails> visStartList = new ArrayList<SelectItemDetails>();
	private ArrayList<SelectItemDetails> visEndList = new ArrayList<SelectItemDetails>();
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
	public ArrayList<SSTMDMVinDetails> getMappingList() {
		return mappingList;
	}
	public void setMappingList(ArrayList<SSTMDMVinDetails> mappingList) {
		this.mappingList = mappingList;
	}
	public ArrayList<SelectItemDetails> getFlagList() {
		return flagList;
	}
	public void setFlagList(ArrayList<SelectItemDetails> flagList) {
		this.flagList = flagList;
	}
	public SSTMDMVinDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(SSTMDMVinDetails fieldDetails) {
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
	public ArrayList<SSTMDMVinDetails> getMappingListToImport() {
		return mappingListToImport;
	}
	public void setMappingListToImport(
			ArrayList<SSTMDMVinDetails> mappingListToImport) {
		this.mappingListToImport = mappingListToImport;
	}
	public String getReportViewPath() {
		return reportViewPath;
	}
	public void setReportViewPath(String reportViewPath) {
		this.reportViewPath = reportViewPath;
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
	public ArrayList<CarlineDetails> getCarlineCodeList() {
		return carlineCodeList;
	}
	public void setCarlineCodeList(ArrayList<CarlineDetails> carlineCodeList) {
		this.carlineCodeList = carlineCodeList;
	}
	public ArrayList<SelectItemDetails> getWmiCodeList() {
		return wmiCodeList;
	}
	public void setWmiCodeList(ArrayList<SelectItemDetails> wmiCodeList) {
		this.wmiCodeList = wmiCodeList;
	}
	public ArrayList<SelectItemDetails> getVdsCodeList() {
		return vdsCodeList;
	}
	public void setVdsCodeList(ArrayList<SelectItemDetails> vdsCodeList) {
		this.vdsCodeList = vdsCodeList;
	}
	public ArrayList<SelectItemDetails> getVisStartList() {
		return visStartList;
	}
	public void setVisStartList(ArrayList<SelectItemDetails> visStartList) {
		this.visStartList = visStartList;
	}
	public ArrayList<SelectItemDetails> getVisEndList() {
		return visEndList;
	}
	public void setVisEndList(ArrayList<SelectItemDetails> visEndList) {
		this.visEndList = visEndList;
	}
	
	

}
