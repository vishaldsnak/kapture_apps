package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.MasterDataLocaleMappingDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class MasterDataLocaleMappingBean {

	private String errorMessage = null;
	private String successMessage = null;
	private String infoMessage = null;
	private ArrayList<SelectItemDetails> marketList = new ArrayList<SelectItemDetails>();
	private ArrayList<SelectItemDetails> masterDataTypesList = new ArrayList<SelectItemDetails>();
	private ArrayList<MasterDataLocaleMappingDetails> mappingList = new ArrayList<MasterDataLocaleMappingDetails>();

	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private MasterDataLocaleMappingDetails fieldDetails = new MasterDataLocaleMappingDetails();
	private String selectedRows = null;
	private boolean showUpdate = false;
	private String displayPageNo = null;
	private String displayPageLength = null;

	private String updatedRows = null;
	private String actionClicked = null;

	private ArrayList<MasterDataLocaleMappingDetails> mappingListToImport = new ArrayList<MasterDataLocaleMappingDetails>();
	private String reportViewPath = null;
	private boolean showReadControls = false;
	private boolean showWriteControls = false;
	
	
	
	public ArrayList<SelectItemDetails> getMarketList() {
		return marketList;
	}
	public void setMarketList(ArrayList<SelectItemDetails> marketList) {
		this.marketList = marketList;
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
	public ArrayList<SelectItemDetails> getMasterDataTypesList() {
		return masterDataTypesList;
	}
	public void setMasterDataTypesList(
			ArrayList<SelectItemDetails> masterDataTypesList) {
		this.masterDataTypesList = masterDataTypesList;
	}
	public ArrayList<MasterDataLocaleMappingDetails> getMappingList() {
		return mappingList;
	}
	public void setMappingList(ArrayList<MasterDataLocaleMappingDetails> mappingList) {
		this.mappingList = mappingList;
	}
	public ArrayList<SelectItemDetails> getFlagList() {
		return flagList;
	}
	public void setFlagList(ArrayList<SelectItemDetails> flagList) {
		this.flagList = flagList;
	}
	public MasterDataLocaleMappingDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(MasterDataLocaleMappingDetails fieldDetails) {
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
	public ArrayList<MasterDataLocaleMappingDetails> getMappingListToImport() {
		return mappingListToImport;
	}
	public void setMappingListToImport(
			ArrayList<MasterDataLocaleMappingDetails> mappingListToImport) {
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
}