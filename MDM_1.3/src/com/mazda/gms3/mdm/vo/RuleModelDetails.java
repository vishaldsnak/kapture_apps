package com.mazda.gms3.mdm.vo;

public class RuleModelDetails {

	private int srNo;
	private Long ruleModelMappingId=null;
	private Long ruleId=null;
	private String carCode=null;
	private String wmiCode=null;
	private String vdsCode=null;
	private String visStartRange=null;
	private String visEndRange=null;
	
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getRuleModelMappingId() {
		return ruleModelMappingId;
	}
	public void setRuleModelMappingId(Long ruleModelMappingId) {
		this.ruleModelMappingId = ruleModelMappingId;
	}
	public Long getRuleId() {
		return ruleId;
	}
	public void setRuleId(Long ruleId) {
		this.ruleId = ruleId;
	}
	public String getCarCode() {
		return carCode;
	}
	public void setCarCode(String carCode) {
		this.carCode = carCode;
	}
	public String getWmiCode() {
		return wmiCode;
	}
	public void setWmiCode(String wmiCode) {
		this.wmiCode = wmiCode;
	}
	public String getVdsCode() {
		return vdsCode;
	}
	public void setVdsCode(String vdsCode) {
		this.vdsCode = vdsCode;
	}
	public String getVisStartRange() {
		return visStartRange;
	}
	public void setVisStartRange(String visStartRange) {
		this.visStartRange = visStartRange;
	}
	public String getVisEndRange() {
		return visEndRange;
	}
	public void setVisEndRange(String visEndRange) {
		this.visEndRange = visEndRange;
	}
	
}