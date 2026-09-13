package com.webapp.mazda.assetstool.bean;

import java.util.ArrayList;
import java.util.Map;

import com.webapp.mazda.assetstool.vo.SelectItemDetails;

public class ManageAssetsBean {

	private String infoMessage = null;
	private String errorMessage = null;
	private boolean showLocale = false;
	private ArrayList<Map<Object, Object>> uploadedFileList = new ArrayList<Map<Object, Object>>();

	private ArrayList<SelectItemDetails> channelsList = null;
	private String channelName = null;

	private ArrayList<SelectItemDetails> localeList = null;
	private String localeName = null;

	private String actionClicked = null;
	private String selectedRows = null;
	private String displayPageNo=null;
	private String displayPageLength=null;
	

	/*
	 * Added to develop UI Flow 2
	 */
	private String acceptableFilesTypesForSelectedChannel = null;

	public String getAcceptableFilesTypesForSelectedChannel() {
		return acceptableFilesTypesForSelectedChannel;
	}

	public void setAcceptableFilesTypesForSelectedChannel(
			String acceptableFilesTypesForSelectedChannel) {
		this.acceptableFilesTypesForSelectedChannel = acceptableFilesTypesForSelectedChannel;
	}

	public String getActionClicked() {
		return actionClicked;
	}

	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
	}

	public String getInfoMessage() {
		return infoMessage;
	}

	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public ArrayList<SelectItemDetails> getChannelsList() {
		return channelsList;
	}

	public void setChannelsList(ArrayList<SelectItemDetails> channelsList) {
		this.channelsList = channelsList;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public ArrayList<SelectItemDetails> getLocaleList() {
		return localeList;
	}

	public void setLocaleList(ArrayList<SelectItemDetails> localeList) {
		this.localeList = localeList;
	}

	public String getLocaleName() {
		return localeName;
	}

	public ArrayList<Map<Object, Object>> getUploadedFileList() {
		return uploadedFileList;
	}

	public void setUploadedFileList(
			ArrayList<Map<Object, Object>> uploadedFileList) {
		this.uploadedFileList = uploadedFileList;
	}

	public void setLocaleName(String localeName) {
		this.localeName = localeName;
	}

	public boolean isShowLocale() {
		return showLocale;
	}

	public void setShowLocale(boolean showLocale) {
		this.showLocale = showLocale;
	}

	public String getSelectedRows() {
		return selectedRows;
	}

	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
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
	
	
}
