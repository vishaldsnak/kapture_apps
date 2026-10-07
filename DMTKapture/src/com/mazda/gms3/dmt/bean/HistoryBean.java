package com.mazda.gms3.dmt.bean;

import java.util.ArrayList;

import com.mazda.gms3.dmt.vo.ScheduleDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;

public class HistoryBean {

	private String errorMessage=null;
	private String successMessage=null;
	private ArrayList<ScheduleDetails> scheduleList = null;
	private ArrayList<ScheduleItemDetails> itemsList = null;
	private ScheduleDetails displayScheduleDetails=null;
	private boolean showDetailsPopUp=false;
	private String displayPageNo =null;
	private String displayPageLength=null;
	private boolean runReloadScript = false;
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
	public ArrayList<ScheduleDetails> getScheduleList() {
		return scheduleList;
	}
	public void setScheduleList(ArrayList<ScheduleDetails> scheduleList) {
		this.scheduleList = scheduleList;
	}
	public ArrayList<ScheduleItemDetails> getItemsList() {
		return itemsList;
	}
	public void setItemsList(ArrayList<ScheduleItemDetails> itemsList) {
		this.itemsList = itemsList;
	}
	public ScheduleDetails getDisplayScheduleDetails() {
		return displayScheduleDetails;
	}
	public void setDisplayScheduleDetails(ScheduleDetails displayScheduleDetails) {
		this.displayScheduleDetails = displayScheduleDetails;
	}
}
