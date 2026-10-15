package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.ESILabelUpdateScheduleDetails;

public class ESILabelUpdateScheduleBean {
	
	private ArrayList<ESILabelUpdateScheduleDetails> transactionList = new ArrayList<ESILabelUpdateScheduleDetails>();
	private boolean showReadControls = false;
	private boolean showWriteControls = false;
	private boolean runReloadScript = false;
	public ArrayList<ESILabelUpdateScheduleDetails> getTransactionList() {
		return transactionList;
	}
	public void setTransactionList(
			ArrayList<ESILabelUpdateScheduleDetails> transactionList) {
		this.transactionList = transactionList;
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
	
	

}
