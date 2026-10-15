package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.SIVINScheduleDetails;

public class MMESIVINBatchTransactionsBean {
	
	private String displayPageNo =null;
	private String displayPageLength = null;
	private String successMessage=null;
	private String errorMessage=null;
	private boolean showReadControls = false;
	private boolean showWriteControls = false;
	private ArrayList<SIVINScheduleDetails> transationsList = new ArrayList<SIVINScheduleDetails>();
	private boolean runReloadScript = false;

	
	
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

	public boolean isRunReloadScript() {
		return runReloadScript;
	}

	public void setRunReloadScript(boolean runReloadScript) {
		this.runReloadScript = runReloadScript;
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
	public ArrayList<SIVINScheduleDetails> getTransationsList() {
		return transationsList;
	}
	public void setTransationsList(ArrayList<SIVINScheduleDetails> transationsList) {
		this.transationsList = transationsList;
	}
}