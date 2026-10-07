package com.mazda.gms3.dmt.metadataval.bean;

import java.util.ArrayList;

import com.mazda.gms3.dmt.metadataval.vo.MetaDataFileDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;

public class MetaDataValidationBean {

	private String errorMessage=null;
	private String successMessage=null;
	private String displayPageNo=null;
	private String displayPageLength =null;
	private String scheduleDisplayPageNo=null;
	private String scheduleDisplayPageLength =null;
	private String actionClicked=null;
	
	private ArrayList<SelectItemDetails> countryList=null;
	private ArrayList<SelectItemDetails> languageList=null;
	
	private String countryId=null;
	private String languageId=null;
	private String languageCode=null;
	
	private ArrayList<SelectItemDetails> metaDataTypeList=null;
	private String metaDataType=null;
	
	private ArrayList<MetaDataFileDetails> itemsList = null;
	private String itemToBeDeleted=null;
	
	private ArrayList<MetaDataFileDetails> scheduleList=null;
	
	private boolean reloadJSP=false;
	
	
	public boolean isReloadJSP() {
		return reloadJSP;
	}

	public void setReloadJSP(boolean reloadJSP) {
		this.reloadJSP = reloadJSP;
	}

	public ArrayList<MetaDataFileDetails> getScheduleList() {
		return scheduleList;
	}

	public void setScheduleList(ArrayList<MetaDataFileDetails> scheduleList) {
		this.scheduleList = scheduleList;
	}

	public String getItemToBeDeleted() {
		return itemToBeDeleted;
	}

	public void setItemToBeDeleted(String itemToBeDeleted) {
		this.itemToBeDeleted = itemToBeDeleted;
	}

	public ArrayList<MetaDataFileDetails> getItemsList() {
		return itemsList;
	}

	public void setItemsList(ArrayList<MetaDataFileDetails> itemsList) {
		this.itemsList = itemsList;
	}

	public ArrayList<SelectItemDetails> getMetaDataTypeList() {
		return metaDataTypeList;
	}

	public void setMetaDataTypeList(ArrayList<SelectItemDetails> metaDataTypeList) {
		this.metaDataTypeList = metaDataTypeList;
	}

	public String getMetaDataType() {
		return metaDataType;
	}

	public void setMetaDataType(String metaDataType) {
		this.metaDataType = metaDataType;
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

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	public ArrayList<SelectItemDetails> getCountryList() {
		return countryList;
	}

	public void setCountryList(ArrayList<SelectItemDetails> countryList) {
		this.countryList = countryList;
	}

	public ArrayList<SelectItemDetails> getLanguageList() {
		return languageList;
	}

	public void setLanguageList(ArrayList<SelectItemDetails> languageList) {
		this.languageList = languageList;
	}

	public String getCountryId() {
		return countryId;
	}

	public void setCountryId(String countryId) {
		this.countryId = countryId;
	}

	public String getLanguageId() {
		return languageId;
	}

	public void setLanguageId(String languageId) {
		this.languageId = languageId;
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

	public String getScheduleDisplayPageNo() {
		return scheduleDisplayPageNo;
	}

	public void setScheduleDisplayPageNo(String scheduleDisplayPageNo) {
		this.scheduleDisplayPageNo = scheduleDisplayPageNo;
	}

	public String getScheduleDisplayPageLength() {
		return scheduleDisplayPageLength;
	}

	public void setScheduleDisplayPageLength(String scheduleDisplayPageLength) {
		this.scheduleDisplayPageLength = scheduleDisplayPageLength;
	}
	
	
	
	
}
