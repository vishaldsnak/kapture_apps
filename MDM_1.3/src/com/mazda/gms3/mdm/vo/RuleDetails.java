package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;
import java.util.ArrayList;

public class RuleDetails {

	private int srNo;
	private Long ruleId=null;
	private String ruleName=null;
	private String ruleDesc=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private boolean defaultRule = false;
	
	private ArrayList<RuleModelDetails> modelMappingList= new ArrayList<RuleModelDetails>();
	private ArrayList<RuleLanguageDetails> languageMappingList = new ArrayList<RuleLanguageDetails>();
	
	private boolean showCheckBox = true;
	
	public boolean isShowCheckBox() {
		return showCheckBox;
	}
	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}

	
	
	public boolean isDefaultRule() {
		return defaultRule;
	}
	public void setDefaultRule(boolean defaultRule) {
		this.defaultRule = defaultRule;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getRuleId() {
		return ruleId;
	}
	public void setRuleId(Long ruleId) {
		this.ruleId = ruleId;
	}
	public String getRuleName() {
		return ruleName;
	}
	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}
	public String getRuleDesc() {
		return ruleDesc;
	}
	public void setRuleDesc(String ruleDesc) {
		this.ruleDesc = ruleDesc;
	}
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	public String getFlagLabel() {
		return flagLabel;
	}
	public void setFlagLabel(String flagLabel) {
		this.flagLabel = flagLabel;
	}
	public Timestamp getEntryTime() {
		return entryTime;
	}
	public void setEntryTime(Timestamp entryTime) {
		this.entryTime = entryTime;
	}
	public Timestamp getUpdatedTime() {
		return updatedTime;
	}
	public void setUpdatedTime(Timestamp updatedTime) {
		this.updatedTime = updatedTime;
	}
	public boolean isEditableFlag() {
		return editableFlag;
	}
	public void setEditableFlag(boolean editableFlag) {
		this.editableFlag = editableFlag;
	}
	public ArrayList<RuleModelDetails> getModelMappingList() {
		return modelMappingList;
	}
	public void setModelMappingList(ArrayList<RuleModelDetails> modelMappingList) {
		this.modelMappingList = modelMappingList;
	}
	public ArrayList<RuleLanguageDetails> getLanguageMappingList() {
		return languageMappingList;
	}
	public void setLanguageMappingList(
			ArrayList<RuleLanguageDetails> languageMappingList) {
		this.languageMappingList = languageMappingList;
	}
}