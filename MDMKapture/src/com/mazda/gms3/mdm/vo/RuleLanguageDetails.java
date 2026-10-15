package com.mazda.gms3.mdm.vo;

public class RuleLanguageDetails {
	
	private int srNo;
	private Long ruleLanguageMappingId=null;
	private Long ruleId=null;
	private String fromLangCode=null;
	private String fromLangName=null;
	private String toLangCode=null;
	private String toLangName=null;
	private String refType=null;
	
	
	public String getRefType() {
		return refType;
	}
	public void setRefType(String refType) {
		this.refType = refType;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getRuleLanguageMappingId() {
		return ruleLanguageMappingId;
	}
	public void setRuleLanguageMappingId(Long ruleLanguageMappingId) {
		this.ruleLanguageMappingId = ruleLanguageMappingId;
	}
	public Long getRuleId() {
		return ruleId;
	}
	public void setRuleId(Long ruleId) {
		this.ruleId = ruleId;
	}
	public String getFromLangCode() {
		return fromLangCode;
	}
	public void setFromLangCode(String fromLangCode) {
		this.fromLangCode = fromLangCode;
	}
	public String getFromLangName() {
		return fromLangName;
	}
	public void setFromLangName(String fromLangName) {
		this.fromLangName = fromLangName;
	}
	public String getToLangCode() {
		return toLangCode;
	}
	public void setToLangCode(String toLangCode) {
		this.toLangCode = toLangCode;
	}
	public String getToLangName() {
		return toLangName;
	}
	public void setToLangName(String toLangName) {
		this.toLangName = toLangName;
	}
}