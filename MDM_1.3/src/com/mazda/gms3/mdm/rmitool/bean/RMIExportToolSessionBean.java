package com.mazda.gms3.mdm.rmitool.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolItemDetails;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;

public class RMIExportToolSessionBean {
	
	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage = null;
	private String[] countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String[] manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	
	private ArrayList<CarlineDetails> modelsList = new ArrayList<CarlineDetails>();
	private String[] modelId = null;
	
	private String actionClicked=null;
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	private String displayPageNo=null;
	private String displayPageLength=null;
	private String selectedRows=null;
	private ArrayList<RMIExportToolItemDetails> itemsList = null;
	private ArrayList<RMIExportToolItemDetails> selectedItemsList = null;
	
	private String scheduleName=null;
	
	
	public String getScheduleName() {
		return scheduleName;
	}
	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}
	public ArrayList<RMIExportToolItemDetails> getSelectedItemsList() {
		return selectedItemsList;
	}
	public void setSelectedItemsList(ArrayList<RMIExportToolItemDetails> selectedItemsList) {
		this.selectedItemsList = selectedItemsList;
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
	public String[] getCountryLocaleId() {
		return countryLocaleId;
	}
	public void setCountryLocaleId(String[] countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
	}
	public ArrayList<CountryLocaleDetails> getCountryLocaleList() {
		return countryLocaleList;
	}
	public void setCountryLocaleList(ArrayList<CountryLocaleDetails> countryLocaleList) {
		this.countryLocaleList = countryLocaleList;
	}
	public String[] getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(String[] manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
	}
	public ArrayList<ManualLanguageDetails> getLanguageList() {
		return languageList;
	}
	public void setLanguageList(ArrayList<ManualLanguageDetails> languageList) {
		this.languageList = languageList;
	}
	public ArrayList<CarlineDetails> getModelsList() {
		return modelsList;
	}
	public void setModelsList(ArrayList<CarlineDetails> modelsList) {
		this.modelsList = modelsList;
	}
	public String[] getModelId() {
		return modelId;
	}
	public void setModelId(String[] modelId) {
		this.modelId = modelId;
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
	public String getSelectedRows() {
		return selectedRows;
	}
	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
	}
	public ArrayList<RMIExportToolItemDetails> getItemsList() {
		return itemsList;
	}
	public void setItemsList(ArrayList<RMIExportToolItemDetails> itemsList) {
		this.itemsList = itemsList;
	}
	
	
}
