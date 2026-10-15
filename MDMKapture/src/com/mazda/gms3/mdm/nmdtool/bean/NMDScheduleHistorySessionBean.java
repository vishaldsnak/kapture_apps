package com.mazda.gms3.mdm.nmdtool.bean;

import java.util.ArrayList;
import java.util.List;

import com.mazda.gms3.mdm.nmdtool.vo.NMDJobDetails;

public class NMDScheduleHistorySessionBean {
	
	private String displayPageNo =null;
	private String displayPageLength = null;
	private String successMessage=null;
	private String errorMessage=null;
	private boolean showReadControls = false;
	private boolean showWriteControls = false;
	private List<NMDJobDetails> transationsList = new ArrayList<NMDJobDetails>();
	private boolean runReloadScript = false;
	private String actionClicked=null;
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
	public String getActionClicked() {
		return actionClicked;
	}
	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
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
	public String getSuccessMessage() {
		return successMessage;
	}
	public void setSuccessMessage(String successMessage) {
		this.successMessage = successMessage;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
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
	public List<NMDJobDetails> getTransationsList() {
		return transationsList;
	}
	public void setTransationsList(List<NMDJobDetails> transationsList) {
		this.transationsList = transationsList;
	}
	public boolean isRunReloadScript() {
		return runReloadScript;
	}
	public void setRunReloadScript(boolean runReloadScript) {
		this.runReloadScript = runReloadScript;
	}
}