package com.mazda.gms3.mdm.nmdtool.bean;

import java.sql.Timestamp;
import java.util.List;

import com.mazda.gms3.mdm.nmdtool.vo.NMDScheduleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class NMDScheduleSessionBean {
	
	private String errorMessage=null;
	private String successMessage=null;
	
	private String dayOfMonth=null;
	private String timeOfMonth=null;
	private List<NMDScheduleDetails> schedulesList = null;

	private List<SelectItemDetails> daysList=null;
	private List<SelectItemDetails> hoursList=null;
	
	private String actionClicked=null;
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
	private String displayPageNo=null;
	private String displayPageLength=null;
	private String selectedRows=null;
	
	private String deltaLastExecTime=null;
	private Timestamp convDeltaLastExecTime=null;
	
	public Timestamp getConvDeltaLastExecTime() {
		return convDeltaLastExecTime;
	}
	public void setConvDeltaLastExecTime(Timestamp convDeltaLastExecTime) {
		this.convDeltaLastExecTime = convDeltaLastExecTime;
	}
	public String getDeltaLastExecTime() {
		return deltaLastExecTime;
	}
	public void setDeltaLastExecTime(String deltaLastExecTime) {
		this.deltaLastExecTime = deltaLastExecTime;
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
	public String getDayOfMonth() {
		return dayOfMonth;
	}
	public void setDayOfMonth(String dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}
	public String getTimeOfMonth() {
		return timeOfMonth;
	}
	public void setTimeOfMonth(String timeOfMonth) {
		this.timeOfMonth = timeOfMonth;
	}
	public List<NMDScheduleDetails> getSchedulesList() {
		return schedulesList;
	}
	public void setSchedulesList(List<NMDScheduleDetails> schedulesList) {
		this.schedulesList = schedulesList;
	}
	public List<SelectItemDetails> getDaysList() {
		return daysList;
	}
	public void setDaysList(List<SelectItemDetails> daysList) {
		this.daysList = daysList;
	}
	public List<SelectItemDetails> getHoursList() {
		return hoursList;
	}
	public void setHoursList(List<SelectItemDetails> hoursList) {
		this.hoursList = hoursList;
	}
	
	
}
