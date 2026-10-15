package com.mazda.gms3.cdrom.bean;

import java.util.ArrayList;
import java.util.List;

import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;

public class CDRomSearchBean {

	private String errorMessage = null;
	private String scheduleID;

	// Page 1 Properties
	List<LabelBean> models = new ArrayList<LabelBean>();
	List<LabelBean> wmi = new ArrayList<LabelBean>();
	List<LabelBean> vds = new ArrayList<LabelBean>();
	List<LabelBean> vinRange = new ArrayList<LabelBean>();
	List<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	List<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();

	String selectedCountry;
	String selectedLanguage;
	String selectedModel;
	String selectedWmi;
	String selectedVds;
	String selectedVinRange;
	String actionClicked=null;
	// Page2 Properties
	List<LabelBean> serviceContents = new ArrayList<LabelBean>();
	List<LabelBean> engineWorkshopManuals = new ArrayList<LabelBean>();
	List<LabelBean> transmissionWorkshopManual = new ArrayList<LabelBean>();

	List<String> selectedServiceContents = new ArrayList<String>();
	List<String> selectedEngineWorkshopManuals = new ArrayList<String>();
	List<String> selectedTransmissionWorkshopManual = new ArrayList<String>();

	private String networkPath = null;

	private boolean showReadControls = false;
	private boolean showWriteControls = false;
	private ArrayList<ApplicableVINList> applicableVINList =new ArrayList<ApplicableVINList>();
	private ArrayList<CDRomScheduleItemDetails> itemsList = new ArrayList<CDRomScheduleItemDetails>();
	
	private ArrayList<CDRomScheduleDetails> searchCritriaJobList = new ArrayList<CDRomScheduleDetails>();
	private String displayPageNo = null;
	private String displayPageLength = null;
	private boolean runReloadScript=false;
	
	private String fromDate = null;
	private String toDate = null;
	
	
	
	public String getFromDate() {
		return fromDate;
	}
	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}
	public String getToDate() {
		return toDate;
	}
	public void setToDate(String toDate) {
		this.toDate = toDate;
	}
	
	public boolean isRunReloadScript() {
		return runReloadScript;
	}

	public void setRunReloadScript(boolean runReloadScript) {
		this.runReloadScript = runReloadScript;
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

	public ArrayList<CDRomScheduleDetails> getSearchCritriaJobList() {
		return searchCritriaJobList;
	}

	public void setSearchCritriaJobList(ArrayList<CDRomScheduleDetails> searchCritriaJobList) {
		this.searchCritriaJobList = searchCritriaJobList;
	}

	public ArrayList<CDRomScheduleItemDetails> getItemsList() {
		return itemsList;
	}

	public void setItemsList(ArrayList<CDRomScheduleItemDetails> itemsList) {
		this.itemsList = itemsList;
	}

	public ArrayList<ApplicableVINList> getApplicableVINList() {
		return applicableVINList;
	}

	public void setApplicableVINList(ArrayList<ApplicableVINList> applicableVINList) {
		this.applicableVINList = applicableVINList;
	}

	public String getActionClicked() {
		return actionClicked;
	}

	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public String getScheduleID() {
		return scheduleID;
	}

	public void setScheduleID(String scheduleID) {
		this.scheduleID = scheduleID;
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

	public String getNetworkPath() {
		return networkPath;
	}

	public void setNetworkPath(String networkPath) {
		this.networkPath = networkPath;
	}

	public List<LabelBean> getModels() {
		return models;
	}

	public void setModels(List<LabelBean> models) {
		this.models = models;
	}

	public List<LabelBean> getWmi() {
		return wmi;
	}

	public void setWmi(List<LabelBean> wmi) {
		this.wmi = wmi;
	}

	public List<LabelBean> getVds() {
		return vds;
	}

	public void setVds(List<LabelBean> vds) {
		this.vds = vds;
	}

	public List<LabelBean> getVinRange() {
		return vinRange;
	}

	public void setVinRange(List<LabelBean> vinRange) {
		this.vinRange = vinRange;
	}

	public String getSelectedCountry() {
		return selectedCountry;
	}

	public void setSelectedCountry(String selectedCountry) {
		this.selectedCountry = selectedCountry;
	}

	public String getSelectedModel() {
		return selectedModel;
	}

	public void setSelectedModel(String selectedModel) {
		this.selectedModel = selectedModel;
	}

	public String getSelectedWmi() {
		return selectedWmi;
	}

	public void setSelectedWmi(String slelectedWmi) {
		this.selectedWmi = slelectedWmi;
	}

	public String getSelectedVds() {
		return selectedVds;
	}

	public void setSelectedVds(String selectedVds) {
		this.selectedVds = selectedVds;
	}

	public String getSelectedVinRange() {
		return selectedVinRange;
	}

	public void setSelectedVinRange(String selectedvinRange) {
		this.selectedVinRange = selectedvinRange;
	}

	public String getSelectedLanguage() {
		return selectedLanguage;
	}

	public void setSelectedLanguage(String selectedLanguage) {
		this.selectedLanguage = selectedLanguage;
	}

	public void reset() {
		errorMessage = null;
		selectedLanguage = null;
		selectedModel = null;
		selectedWmi = null;
		selectedVds = null;
		selectedVinRange = null;
		models = new ArrayList<LabelBean>();
		wmi = new ArrayList<LabelBean>();
		vds = new ArrayList<LabelBean>();
		vinRange = new ArrayList<LabelBean>();
		languageList = new ArrayList<ManualLanguageDetails>();
		resetPage2();
	}

	public void resetPage2() {

		serviceContents = new ArrayList<LabelBean>();
		engineWorkshopManuals = new ArrayList<LabelBean>();
		transmissionWorkshopManual = new ArrayList<LabelBean>();

		selectedServiceContents = new ArrayList<String>();
		selectedEngineWorkshopManuals = new ArrayList<String>();
		selectedTransmissionWorkshopManual = new ArrayList<String>();

	}

	public List<ManualLanguageDetails> getLanguageList() {
		return languageList;
	}

	public void setLanguageList(List<ManualLanguageDetails> languageList) {
		this.languageList = languageList;
	}

	public List<CountryLocaleDetails> getCountryLocaleList() {
		return countryLocaleList;
	}

	public void setCountryLocaleList(List<CountryLocaleDetails> countryLocaleList) {
		this.countryLocaleList = countryLocaleList;
	}
	
	public List<LabelBean> getServiceContents() {
		return serviceContents;
	}

	public void setServiceContents(List<LabelBean> serviceContents) {
		this.serviceContents = serviceContents;
	}

	public List<LabelBean> getEngineWorkshopManuals() {
		return engineWorkshopManuals;
	}

	public void setEngineWorkshopManuals(List<LabelBean> engineWorkshopManuals) {
		this.engineWorkshopManuals = engineWorkshopManuals;
	}

	public List<LabelBean> getTransmissionWorkshopManual() {
		return transmissionWorkshopManual;
	}

	public void setTransmissionWorkshopManual(
			List<LabelBean> transmissionWorkshopManual) {
		this.transmissionWorkshopManual = transmissionWorkshopManual;
	}

	public List<String> getSelectedServiceContents() {
		return selectedServiceContents;
	}

	public void setSelectedServiceContents(List<String> selectedServiceContents) {
		this.selectedServiceContents = selectedServiceContents;
	}

	public List<String> getSelectedEngineWorkshopManuals() {
		return selectedEngineWorkshopManuals;
	}

	public void setSelectedEngineWorkshopManuals(
			List<String> selectedEngineWorkshopManuals) {
		this.selectedEngineWorkshopManuals = selectedEngineWorkshopManuals;
	}

	public List<String> getSelectedTransmissionWorkshopManual() {
		return selectedTransmissionWorkshopManual;
	}

	public void setSelectedTransmissionWorkshopManual(
			List<String> selectedTransmissionWorkshopManual) {
		this.selectedTransmissionWorkshopManual = selectedTransmissionWorkshopManual;
	}

	// TODO add the methods for all validaion and other stuff in this place.
}
