package com.mazda.gms3.mdm.bean;

import java.util.List;

import com.mazda.gms3.mdm.vo.SchVINMappingDetails;

public class SchMaintenanceVINMappingBean {

	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	private String errorMessage=null;
	private String successMessage=null;
	private String infoMessage = null;
	
	
	private String selectedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private String actionClicked=null;
	private String updatedRows=null;
	
	private List<SchVINMappingDetails> schVinMappingList = null;
	private List<SchVINMappingDetails> schVinMappingListToImport=null;
	
	private String reportViewPath = null;
	
	
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
	public List<SchVINMappingDetails> getSchVinMappingList() {
		return schVinMappingList;
	}
	public void setSchVinMappingList(List<SchVINMappingDetails> schVinMappingList) {
		this.schVinMappingList = schVinMappingList;
	}
	public List<SchVINMappingDetails> getSchVinMappingListToImport() {
		return schVinMappingListToImport;
	}
	public void setSchVinMappingListToImport(List<SchVINMappingDetails> schVinMappingListToImport) {
		this.schVinMappingListToImport = schVinMappingListToImport;
	}
	
	
}
