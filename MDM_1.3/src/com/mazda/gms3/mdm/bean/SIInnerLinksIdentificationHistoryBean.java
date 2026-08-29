package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.SIInnerLinksIdentificationTransactionDetails;

public class SIInnerLinksIdentificationHistoryBean {

	private ArrayList<SIInnerLinksIdentificationTransactionDetails> historyList = new ArrayList<SIInnerLinksIdentificationTransactionDetails>();
	private boolean showReadControls = false;
	private boolean showWriteControls = false;
	private boolean runReloadScript = false;

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

	public ArrayList<SIInnerLinksIdentificationTransactionDetails> getHistoryList() {
		return historyList;
	}

	public void setHistoryList(
			ArrayList<SIInnerLinksIdentificationTransactionDetails> historyList) {
		this.historyList = historyList;
	}
}