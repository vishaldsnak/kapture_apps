package com.mazda.gms3.dmt.bean;

import java.util.ArrayList;
import java.util.List;

import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.vo.ScheduleDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;

public class ScheduleBean {
	
	private String errorMessage=null;
	private String successMessage=null;
	private String scheduleName=null;
	private String scheduleATName=null;
	private ArrayList<SelectItemDetails> marketList = new ArrayList<SelectItemDetails>();
	private String marketId=null;
	private ArrayList<SelectItemDetails> localesList = new ArrayList<SelectItemDetails>();
	private String selectedLocalesString=null;
	private String[] selectedLocales = null;
	private ArrayList<SelectItemDetails> modelsList = new ArrayList<SelectItemDetails>();
	private String selectedModelsString=null;
	private String[] selectedModels=null;
	private String documentStatusId=null;
	// "Master Data with Content" or "Only Content" - see ScheduleDAO.LOAD_TYPE_...
	private String loadTypeId=null;
	private ArrayList<SelectItemDetails> documentStatusList = new ArrayList<SelectItemDetails>();
	private ArrayList<ScheduleItemDetails> scheduleItemsList = new ArrayList<ScheduleItemDetails>();
	private boolean selectAll=false;
	private boolean selectATAll=false;
	private String displayPageNo=null;
	private String displayPageLength =null;
	private String displayATPageNo=null;
	private String displayATPageLength =null;
	
	private boolean showJSPData = false;
	
	private boolean showNoRecordTable=false;
	private String selectedRows=null;
	private String selectedATRows=null;
	
	private ArrayList<SelectItemDetails> manualTypesList = new ArrayList<SelectItemDetails>();
	private String[] selectedManualTypes=null;
	private String selectedManualTypesString=null;
	
	
	private ArrayList<SelectItemDetails> operationList = new ArrayList<SelectItemDetails>();
	private String operationId=null;
	private String actionClicked=null;
	private ArrayList<ItemDetails> automationItemsList = new ArrayList<ItemDetails>();
	private boolean showAutomationGridBlock=false;
	private ArrayList<ExcelRowDetails> inputDataList = new ArrayList<ExcelRowDetails>();
	private boolean showIdenitfyButton = false;
	
	
	private String toDate=null;
	private String fromDate=null;
	private List<ScheduleDetails> atDocsIdentificationJobList=null;
	private String jobIdForATOperation=null;
	private String displayATDCIDPageNo=null;
	private String displayATDCIDPageLength = null;
	
	
	
	
	public String getDisplayATDCIDPageNo() {
		return displayATDCIDPageNo;
	}


	public void setDisplayATDCIDPageNo(String displayATDCIDPageNo) {
		this.displayATDCIDPageNo = displayATDCIDPageNo;
	}


	public String getDisplayATDCIDPageLength() {
		return displayATDCIDPageLength;
	}


	public void setDisplayATDCIDPageLength(String displayATDCIDPageLength) {
		this.displayATDCIDPageLength = displayATDCIDPageLength;
	}


	public String getJobIdForATOperation() {
		return jobIdForATOperation;
	}


	public void setJobIdForATOperation(String jobIdForATOperation) {
		this.jobIdForATOperation = jobIdForATOperation;
	}


	public String getToDate() {
		return toDate;
	}


	public void setToDate(String toDate) {
		this.toDate = toDate;
	}


	public String getFromDate() {
		return fromDate;
	}


	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}


	public List<ScheduleDetails> getAtDocsIdentificationJobList() {
		return atDocsIdentificationJobList;
	}


	public void setAtDocsIdentificationJobList(List<ScheduleDetails> atDocsIdentificationJobList) {
		this.atDocsIdentificationJobList = atDocsIdentificationJobList;
	}


	public ArrayList<ExcelRowDetails> getInputDataList() {
		return inputDataList;
	}


	public void setInputDataList(ArrayList<ExcelRowDetails> inputDataList) {
		this.inputDataList = inputDataList;
	}


	public boolean isShowIdenitfyButton() {
		return showIdenitfyButton;
	}


	public void setShowIdenitfyButton(boolean showIdenitfyButton) {
		this.showIdenitfyButton = showIdenitfyButton;
	}


	public String getScheduleATName() {
		return scheduleATName;
	}


	public void setScheduleATName(String scheduleATName) {
		this.scheduleATName = scheduleATName;
	}


	public boolean isSelectATAll() {
		return selectATAll;
	}


	public void setSelectATAll(boolean selectATAll) {
		this.selectATAll = selectATAll;
	}


	public String getSelectedATRows() {
		return selectedATRows;
	}


	public void setSelectedATRows(String selectedATRows) {
		this.selectedATRows = selectedATRows;
	}


	public String getDisplayATPageNo() {
		return displayATPageNo;
	}


	public void setDisplayATPageNo(String displayATPageNo) {
		this.displayATPageNo = displayATPageNo;
	}


	public String getDisplayATPageLength() {
		return displayATPageLength;
	}


	public void setDisplayATPageLength(String displayATPageLength) {
		this.displayATPageLength = displayATPageLength;
	}


	public boolean isShowAutomationGridBlock() {
		return showAutomationGridBlock;
	}


	public void setShowAutomationGridBlock(boolean showAutomationGridBlock) {
		this.showAutomationGridBlock = showAutomationGridBlock;
	}


	public ArrayList<ItemDetails> getAutomationItemsList() {
		return automationItemsList;
	}


	public void setAutomationItemsList(ArrayList<ItemDetails> automationItemsList) {
		this.automationItemsList = automationItemsList;
	}


	public String getSelectedLocalesString() {
		return selectedLocalesString;
	}


	public void setSelectedLocalesString(String selectedLocalesString) {
		this.selectedLocalesString = selectedLocalesString;
	}


	public String getSelectedModelsString() {
		return selectedModelsString;
	}


	public String getActionClicked() {
		return actionClicked;
	}


	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
	}


	public void setSelectedModelsString(String selectedModelsString) {
		this.selectedModelsString = selectedModelsString;
	}


	public String getSelectedManualTypesString() {
		return selectedManualTypesString;
	}


	public void setSelectedManualTypesString(String selectedManualTypesString) {
		this.selectedManualTypesString = selectedManualTypesString;
	}


	public ArrayList<SelectItemDetails> getOperationList() {
		return operationList;
	}


	public void setOperationList(ArrayList<SelectItemDetails> operationList) {
		this.operationList = operationList;
	}


	public String getOperationId() {
		return operationId;
	}


	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}


	public ArrayList<SelectItemDetails> getManualTypesList() {
		return manualTypesList;
	}


	public void setManualTypesList(ArrayList<SelectItemDetails> manualTypesList) {
		this.manualTypesList = manualTypesList;
	}


	public String[] getSelectedManualTypes() {
		return selectedManualTypes;
	}


	public void setSelectedManualTypes(String[] selectedManualTypes) {
		this.selectedManualTypes = selectedManualTypes;
	}


	public String getSelectedRows() {
		return selectedRows;
	}


	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
	}


	public boolean isShowNoRecordTable() {
		return showNoRecordTable;
	}


	public void setShowNoRecordTable(boolean showNoRecordTable) {
		this.showNoRecordTable = showNoRecordTable;
	}


	public boolean isShowJSPData() {
		return showJSPData;
	}


	public void setShowJSPData(boolean showJSPData) {
		this.showJSPData = showJSPData;
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


	public String getScheduleName() {
		return scheduleName;
	}


	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}


	public boolean isSelectAll() {
		return selectAll;
	}


	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}


	public String getDocumentStatusId() {
		return documentStatusId;
	}


	public void setDocumentStatusId(String documentStatusId) {
		this.documentStatusId = documentStatusId;
	}


	public ArrayList<SelectItemDetails> getDocumentStatusList() {
		return documentStatusList;
	}


	public void setDocumentStatusList(
			ArrayList<SelectItemDetails> documentStatusList) {
		this.documentStatusList = documentStatusList;
	}


	public String[] getSelectedLocales() {
		return selectedLocales;
	}


	public void setSelectedLocales(String[] selectedLocales) {
		this.selectedLocales = selectedLocales;
	}


	public String[] getSelectedModels() {
		return selectedModels;
	}


	public void setSelectedModels(String[] selectedModels) {
		this.selectedModels = selectedModels;
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


	public ArrayList<SelectItemDetails> getMarketList() {
		return marketList;
	}


	public void setMarketList(ArrayList<SelectItemDetails> marketList) {
		this.marketList = marketList;
	}


	public String getMarketId() {
		return marketId;
	}


	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}


	public ArrayList<SelectItemDetails> getLocalesList() {
		return localesList;
	}


	public void setLocalesList(ArrayList<SelectItemDetails> localesList) {
		this.localesList = localesList;
	}


	public ArrayList<SelectItemDetails> getModelsList() {
		return modelsList;
	}


	public void setModelsList(ArrayList<SelectItemDetails> modelsList) {
		this.modelsList = modelsList;
	}


	public ArrayList<ScheduleItemDetails> getScheduleItemsList() {
		return scheduleItemsList;
	}


	public void setScheduleItemsList(
			ArrayList<ScheduleItemDetails> scheduleItemsList) {
		this.scheduleItemsList = scheduleItemsList;
	}
	
	
	
	

	public String getLoadTypeId() {
		return loadTypeId;
	}

	public void setLoadTypeId(String loadTypeId) {
		this.loadTypeId = loadTypeId;
	}
}
