package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class MMESIVinBean {
	
	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage=null;
	private String selectedRows=null;
	private String selectedPreviewRows=null;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private String displayPreviewPageNo =null;
	private String displayPreviewPageLength=null;
	
	private String countryLocaleId=null;
	private ArrayList<CountryLocaleDetails> countryLocaleList = new ArrayList<CountryLocaleDetails>();
	private String manualLanguageId=null;
	private ArrayList<ManualLanguageDetails> languageList = new ArrayList<ManualLanguageDetails>();
	
	private ArrayList<SelectItemDetails> wmiList  = new ArrayList<SelectItemDetails>();
	private String wmiId=null;
	
	private String siNumber=null;
	
	private ArrayList<CarlineDetails> modelsList = new ArrayList<CarlineDetails>();
	private String modelId=null;
	private String carlineCode=null;
	private String carlineNameEng=null;
	private String carlineNameReg=null;
	
	private String hiddenEngineTypeId=null;
	private String[] engineTypeIds=null;
	private String hiddenMissionTypeId=null;
	private String[] missionTypeIds=null;
	private ArrayList<SelectItemDetails> engineTypeList = new ArrayList<SelectItemDetails>();
	private ArrayList<SelectItemDetails> missionTypeList = new ArrayList<SelectItemDetails>();
	
	private ArrayList<SelectItemDetails> vdsList  = new ArrayList<SelectItemDetails>();
	private String hiddenVDSId=null;
	private String[] vdsId=null;
	
	private String vinStartRange="000000";
	private String vinEndRange="ZZZZZZ";
	
	private ArrayList<SIVinDetails> documentsList = new ArrayList<SIVinDetails>();
	
	private String actionClicked=null;
	
	private ArrayList<SIVinDetails> tempVinsList = new ArrayList<SIVinDetails>();

	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	private ArrayList<SIVinDetails> vinToImportList = new ArrayList<SIVinDetails>();
	private String reportViewPath=null;
	
	private String scheduleName = null;
	
	private boolean showTranslationOperationsControl=false;
	
	private ArrayList<SelectItemDetails> yesNoList= new ArrayList<SelectItemDetails>();
	private String translationsUpdate=ApplicationProperties.getProperty("value.esicategory.flag.yes");
	
	
	private boolean showAddNewVDSField=false;
	private String addNewVDSId=null;
	
	
	public boolean isShowAddNewVDSField() {
		return showAddNewVDSField;
	}
	public void setShowAddNewVDSField(boolean showAddNewVDSField) {
		this.showAddNewVDSField = showAddNewVDSField;
	}
	public String getAddNewVDSId() {
		return addNewVDSId;
	}
	public void setAddNewVDSId(String addNewVDSId) {
		this.addNewVDSId = addNewVDSId;
	}
	public ArrayList<SelectItemDetails> getYesNoList() {
		return yesNoList;
	}
	public void setYesNoList(ArrayList<SelectItemDetails> yesNoList) {
		this.yesNoList = yesNoList;
	}
	public String getTranslationsUpdate() {
		return translationsUpdate;
	}
	public void setTranslationsUpdate(String translationsUpdate) {
		this.translationsUpdate = translationsUpdate;
	}
	public boolean isShowTranslationOperationsControl() {
		return showTranslationOperationsControl;
	}
	public void setShowTranslationOperationsControl(
			boolean showTranslationOperationsControl) {
		this.showTranslationOperationsControl = showTranslationOperationsControl;
	}
	public String getScheduleName() {
		return scheduleName;
	}
	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}
	public String getHiddenEngineTypeId() {
		return hiddenEngineTypeId;
	}
	public void setHiddenEngineTypeId(String hiddenEngineTypeId) {
		this.hiddenEngineTypeId = hiddenEngineTypeId;
	}
	public String[] getEngineTypeIds() {
		return engineTypeIds;
	}
	public void setEngineTypeIds(String[] engineTypeIds) {
		this.engineTypeIds = engineTypeIds;
	}
	public String getHiddenMissionTypeId() {
		return hiddenMissionTypeId;
	}
	public void setHiddenMissionTypeId(String hiddenMissionTypeId) {
		this.hiddenMissionTypeId = hiddenMissionTypeId;
	}
	public String[] getMissionTypeIds() {
		return missionTypeIds;
	}
	public void setMissionTypeIds(String[] missionTypeIds) {
		this.missionTypeIds = missionTypeIds;
	}
	public ArrayList<SelectItemDetails> getEngineTypeList() {
		return engineTypeList;
	}
	public void setEngineTypeList(ArrayList<SelectItemDetails> engineTypeList) {
		this.engineTypeList = engineTypeList;
	}
	public ArrayList<SelectItemDetails> getMissionTypeList() {
		return missionTypeList;
	}
	public void setMissionTypeList(ArrayList<SelectItemDetails> missionTypeList) {
		this.missionTypeList = missionTypeList;
	}
	public String getReportViewPath() {
		return reportViewPath;
	}
	public void setReportViewPath(String reportViewPath) {
		this.reportViewPath = reportViewPath;
	}
	
	
	public String getHiddenVDSId() {
		return hiddenVDSId;
	}
	public void setHiddenVDSId(String hiddenVDSId) {
		this.hiddenVDSId = hiddenVDSId;
	}
	public String getInfoMessage() {
		return infoMessage;
	}
	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}
	public ArrayList<SIVinDetails> getVinToImportList() {
		return vinToImportList;
	}
	public void setVinToImportList(ArrayList<SIVinDetails> vinToImportList) {
		this.vinToImportList = vinToImportList;
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
	
	public ArrayList<SIVinDetails> getTempVinsList() {
		return tempVinsList;
	}

	public void setTempVinsList(ArrayList<SIVinDetails> tempVinsList) {
		this.tempVinsList = tempVinsList;
	}

	public ArrayList<SelectItemDetails> getWmiList() {
		return wmiList;
	}

	public void setWmiList(ArrayList<SelectItemDetails> wmiList) {
		this.wmiList = wmiList;
	}

	public String getWmiId() {
		return wmiId;
	}

	public void setWmiId(String wmiId) {
		this.wmiId = wmiId;
	}

	public String getSiNumber() {
		return siNumber;
	}

	public void setSiNumber(String siNumber) {
		this.siNumber = siNumber;
	}

	public ArrayList<CarlineDetails> getModelsList() {
		return modelsList;
	}

	public void setModelsList(ArrayList<CarlineDetails> modelsList) {
		this.modelsList = modelsList;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public ArrayList<SelectItemDetails> getVdsList() {
		return vdsList;
	}

	public void setVdsList(ArrayList<SelectItemDetails> vdsList) {
		this.vdsList = vdsList;
	}

	public String[] getVdsId() {
		return vdsId;
	}

	public void setVdsId(String[] vdsId) {
		this.vdsId = vdsId;
	}

	public String getVinStartRange() {
		return vinStartRange;
	}

	public void setVinStartRange(String vinStartRange) {
		this.vinStartRange = vinStartRange;
	}

	public String getVinEndRange() {
		return vinEndRange;
	}

	public void setVinEndRange(String vinEndRange) {
		this.vinEndRange = vinEndRange;
	}

	public ArrayList<SIVinDetails> getDocumentsList() {
		return documentsList;
	}

	public void setDocumentsList(ArrayList<SIVinDetails> documentsList) {
		this.documentsList = documentsList;
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

	public String getSuccessMessage() {
		return successMessage;
	}

	public void setSuccessMessage(String successMessage) {
		this.successMessage = successMessage;
	}

	public String getSelectedRows() {
		return selectedRows;
	}

	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
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

	public String getDisplayPreviewPageNo() {
		return displayPreviewPageNo;
	}

	public void setDisplayPreviewPageNo(String displayPreviewPageNo) {
		this.displayPreviewPageNo = displayPreviewPageNo;
	}

	public String getDisplayPreviewPageLength() {
		return displayPreviewPageLength;
	}

	public void setDisplayPreviewPageLength(String displayPreviewPageLength) {
		this.displayPreviewPageLength = displayPreviewPageLength;
	}

	public String getSelectedPreviewRows() {
		return selectedPreviewRows;
	}

	public void setSelectedPreviewRows(String selectedPreviewRows) {
		this.selectedPreviewRows = selectedPreviewRows;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getCarlineNameEng() {
		return carlineNameEng;
	}
	public void setCarlineNameEng(String carlineNameEng) {
		this.carlineNameEng = carlineNameEng;
	}
	public String getCarlineNameReg() {
		return carlineNameReg;
	}
	public void setCarlineNameReg(String carlineNameReg) {
		this.carlineNameReg = carlineNameReg;
	}
	
	
}