package com.mazda.gms3.cdrom.bean;

import java.util.ArrayList;

public class CDRomHistoryBean {

	private String errorMessage = null;
	private String successMessage = null;
	private ArrayList<CDRomScheduleDetails> scheduleList = null;
	private ArrayList<CDRomScheduleItemDetails> itemsList = null;
	private CDRomScheduleDetails displayScheduleDetails = null;
	private boolean showDetailsPopUp = false;
	private String displayPageNo = null;
	private String displayPageLength = null;
	private boolean runReloadScript = false;
	private boolean showReadControls = false;
	private boolean showWriteControls = false;
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

	public boolean isShowDetailsPopUp() {
		return showDetailsPopUp;
	}

	public void setShowDetailsPopUp(boolean showDetailsPopUp) {
		this.showDetailsPopUp = showDetailsPopUp;
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

	public ArrayList<CDRomScheduleDetails> getScheduleList() {
		return scheduleList;
	}

	public void setScheduleList(ArrayList<CDRomScheduleDetails> scheduleList) {
		this.scheduleList = scheduleList;
	}

	public ArrayList<CDRomScheduleItemDetails> getItemsList() {
		return itemsList;
	}

	public void setItemsList(ArrayList<CDRomScheduleItemDetails> itemsList) {
		this.itemsList = itemsList;
	}

	public CDRomScheduleDetails getDisplayScheduleDetails() {
		return displayScheduleDetails;
	}

	public void setDisplayScheduleDetails(
			CDRomScheduleDetails displayScheduleDetails) {
		this.displayScheduleDetails = displayScheduleDetails;
	}
}
