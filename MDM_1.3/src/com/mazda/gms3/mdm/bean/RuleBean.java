package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.RuleDetails;
import com.mazda.gms3.mdm.vo.RuleLanguageDetails;
import com.mazda.gms3.mdm.vo.RuleModelDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class RuleBean {

	private String errorMessage=null;
	private String successMessage=null;
	private ArrayList<SelectItemDetails> flagList = new ArrayList<SelectItemDetails>();
	private RuleDetails fieldDetails = new RuleDetails();
	private String selectedRows=null;
	private boolean showUpdate=false;
	private String displayPageNo=null;
	private String displayPageLength=null;
	private ArrayList<RuleDetails> ruleList = new ArrayList<RuleDetails>();
	private ArrayList<RuleDetails> ruleListForCombo = new ArrayList<RuleDetails>();
	private String ruleId=null;
	private ArrayList<RuleModelDetails> ruleModelsList = new ArrayList<RuleModelDetails>();
	private ArrayList<RuleLanguageDetails> ruleLanguageList = new ArrayList<RuleLanguageDetails>();
	private ArrayList<RuleLanguageDetails> wdRuleLanguageList = new ArrayList<RuleLanguageDetails>();
	private String actionClicked=null;
	private String modelSrNo=null;
	private String languageSrNo=null;
	private String languageDelType=null;
	private String updatedRows=null;
	private boolean defaultRule = false;
	
	
	
	public boolean isDefaultRule() {
		return defaultRule;
	}
	public void setDefaultRule(boolean defaultRule) {
		this.defaultRule = defaultRule;
	}
	public String getLanguageDelType() {
		return languageDelType;
	}
	public void setLanguageDelType(String languageDelType) {
		this.languageDelType = languageDelType;
	}
	public String getUpdatedRows() {
		return updatedRows;
	}
	public void setUpdatedRows(String updatedRows) {
		this.updatedRows = updatedRows;
	}
	public String getActionClicked() {
		return actionClicked;
	}
	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
	}
	public String getModelSrNo() {
		return modelSrNo;
	}
	public void setModelSrNo(String modelSrNo) {
		this.modelSrNo = modelSrNo;
	}
	public String getLanguageSrNo() {
		return languageSrNo;
	}
	public void setLanguageSrNo(String languageSrNo) {
		this.languageSrNo = languageSrNo;
	}
	public ArrayList<RuleModelDetails> getRuleModelsList() {
		return ruleModelsList;
	}
	public void setRuleModelsList(ArrayList<RuleModelDetails> ruleModelsList) {
		this.ruleModelsList = ruleModelsList;
	}
	public ArrayList<RuleLanguageDetails> getRuleLanguageList() {
		return ruleLanguageList;
	}
	public void setRuleLanguageList(ArrayList<RuleLanguageDetails> ruleLanguageList) {
		this.ruleLanguageList = ruleLanguageList;
	}
	public String getRuleId() {
		return ruleId;
	}
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}
	public ArrayList<RuleDetails> getRuleListForCombo() {
		return ruleListForCombo;
	}
	public void setRuleListForCombo(ArrayList<RuleDetails> ruleListForCombo) {
		this.ruleListForCombo = ruleListForCombo;
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
	public ArrayList<SelectItemDetails> getFlagList() {
		return flagList;
	}
	public void setFlagList(ArrayList<SelectItemDetails> flagList) {
		this.flagList = flagList;
	}
	public RuleDetails getFieldDetails() {
		return fieldDetails;
	}
	public void setFieldDetails(RuleDetails fieldDetails) {
		this.fieldDetails = fieldDetails;
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
	public ArrayList<RuleDetails> getRuleList() {
		return ruleList;
	}
	public void setRuleList(ArrayList<RuleDetails> ruleList) {
		this.ruleList = ruleList;
	}
	public ArrayList<RuleLanguageDetails> getWdRuleLanguageList() {
		return wdRuleLanguageList;
	}
	public void setWdRuleLanguageList(
			ArrayList<RuleLanguageDetails> wdRuleLanguageList) {
		this.wdRuleLanguageList = wdRuleLanguageList;
	}
}