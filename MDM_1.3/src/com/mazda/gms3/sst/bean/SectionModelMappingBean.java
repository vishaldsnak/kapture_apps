package com.mazda.gms3.sst.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.vo.DivisionDetails;
import com.mazda.gms3.sst.vo.SSTDetails;
import com.mazda.gms3.sst.vo.SectionModelMappingDetails;
import com.mazda.gms3.sst.vo.SectionDetails;

public class SectionModelMappingBean {
	
	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String countryLocaleId=null;
	private String divisionId=null; 
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	private ArrayList<CarlineDetails> modelList = new ArrayList<CarlineDetails>();
	private ArrayList<DivisionDetails> divisionList = new ArrayList<DivisionDetails>(); 
	private ArrayList<SectionDetails> sectionList = new ArrayList<SectionDetails>();
	private ArrayList<SectionModelMappingDetails> mappingList = new ArrayList<SectionModelMappingDetails>();
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private SectionModelMappingDetails fieldDetails = new SectionModelMappingDetails();
	private ArrayList<SSTDetails> sstList = new ArrayList<SSTDetails>();
	private String selectedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	
	private String updatedRows = null;
	private String actionClicked = null;
	
	private ArrayList<SectionModelMappingDetails> mappingListToImport = new ArrayList<SectionModelMappingDetails>();
	private String reportViewPath = null;
	
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	
	public String getDivisionId() {
		return divisionId;
	}
	public void setDivisionId(String divisionId) {
		this.divisionId = divisionId;
	}
	public ArrayList<DivisionDetails> getDivisionList() {
		return divisionList;
	}
	public void setDivisionList(ArrayList<DivisionDetails> divisionList) {
		this.divisionList = divisionList;
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
	public ArrayList<SSTDetails> getSstList() {
		return sstList;
	}
	public void setSstList(ArrayList<SSTDetails> sstList) {
		this.sstList = sstList;
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
	public ArrayList<SectionModelMappingDetails> getMappingList() {
		return mappingList;
	}
	public void setMappingList(ArrayList<SectionModelMappingDetails> mappingList) {
		this.mappingList = mappingList;
	}
	public ArrayList<SelectItemDetails> getFlagList() {
		return flagList;
	}
	public void setFlagList(ArrayList<SelectItemDetails> flagList) {
		this.flagList = flagList;
	}
	public SectionModelMappingDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(SectionModelMappingDetails fieldDetails) {
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
	public ArrayList<SectionModelMappingDetails> getMappingListToImport() {
		return mappingListToImport;
	}
	public void setMappingListToImport(
			ArrayList<SectionModelMappingDetails> mappingListToImport) {
		this.mappingListToImport = mappingListToImport;
	}
	public String getReportViewPath() {
		return reportViewPath;
	}
	public void setReportViewPath(String reportViewPath) {
		this.reportViewPath = reportViewPath;
	}
	public ArrayList<CarlineDetails> getModelList() {
		return modelList;
	}
	public void setModelList(ArrayList<CarlineDetails> modelList) {
		this.modelList = modelList;
	}
	public ArrayList<SectionDetails> getSectionList() {
		return sectionList;
	}
	public void setSectionList(ArrayList<SectionDetails> sectionList) {
		this.sectionList = sectionList;
	}
}