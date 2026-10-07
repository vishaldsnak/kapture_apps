package com.mazda.gms3.dmt.vo;

import java.util.ArrayList;


public class VinMLMappingDetails {

	private String localeCode=null;
	private String carlineCode=null;
	private String wmiCode=null;
	private String vdsCode=null;
	private String visStartRange=null;
	private String visEndRange=null;
	
	private String wmMappingLocale=null;
	private String bsmMappingLocale=null;
	private String mcMappingLocale=null;
	private String atMappingLocale=null;
	private String mtMappingLocale=null;
	private String engineMappingLocale=null;
	private String wdMappingLocale=null;
	private String tqgMappingLocale=null;
	private String tgMappingLocale=null;
	private String dmMappingLocale=null;
	private String mcmMappingLocale=null;
	private String rqMappingLocale=null;
	
	private String processingStatus="";
	private String errorCode="";
	private String errorMessage="";
	
	private ScheduleItemDetails itemDetails = null;
	
	private ArrayList<String> manualTypesList=null;
	private String oldCarlineCode=null;
	private String oldWmiCode=null;
	private String oldVdsCode=null;
	private String oldVisStartRange=null;
	private String oldVisEndRange=null;
	
	private String newRefKey=null;
	
	
	public String getDmMappingLocale() {
		return dmMappingLocale;
	}
	public void setDmMappingLocale(String dmMappingLocale) {
		this.dmMappingLocale = dmMappingLocale;
	}
	public String getMcmMappingLocale() {
		return mcmMappingLocale;
	}
	public void setMcmMappingLocale(String mcmMappingLocale) {
		this.mcmMappingLocale = mcmMappingLocale;
	}
	public String getRqMappingLocale() {
		return rqMappingLocale;
	}
	public void setRqMappingLocale(String rqMappingLocale) {
		this.rqMappingLocale = rqMappingLocale;
	}
	public String getNewRefKey() {
		return newRefKey;
	}
	public void setNewRefKey(String newRefKey) {
		this.newRefKey = newRefKey;
	}
	public String getOldCarlineCode() {
		return oldCarlineCode;
	}
	public void setOldCarlineCode(String oldCarlineCode) {
		this.oldCarlineCode = oldCarlineCode;
	}
	public String getOldWmiCode() {
		return oldWmiCode;
	}
	public void setOldWmiCode(String oldWmiCode) {
		this.oldWmiCode = oldWmiCode;
	}
	public String getOldVdsCode() {
		return oldVdsCode;
	}
	public void setOldVdsCode(String oldVdsCode) {
		this.oldVdsCode = oldVdsCode;
	}
	public String getOldVisStartRange() {
		return oldVisStartRange;
	}
	public void setOldVisStartRange(String oldVisStartRange) {
		this.oldVisStartRange = oldVisStartRange;
	}
	public String getOldVisEndRange() {
		return oldVisEndRange;
	}
	public void setOldVisEndRange(String oldVisEndRange) {
		this.oldVisEndRange = oldVisEndRange;
	}
	public ArrayList<String> getManualTypesList() {
		return manualTypesList;
	}
	public void setManualTypesList(ArrayList<String> manualTypesList) {
		this.manualTypesList = manualTypesList;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
	public String getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public ScheduleItemDetails getItemDetails() {
		return itemDetails;
	}
	public void setItemDetails(ScheduleItemDetails itemDetails) {
		this.itemDetails = itemDetails;
	}
	public String getTgMappingLocale() {
		return tgMappingLocale;
	}
	public void setTgMappingLocale(String tgMappingLocale) {
		this.tgMappingLocale = tgMappingLocale;
	}
	public String getLocaleCode() {
		return localeCode;
	}
	public void setLocaleCode(String localeCode) {
		this.localeCode = localeCode;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
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
	public String getWmMappingLocale() {
		return wmMappingLocale;
	}
	public void setWmMappingLocale(String wmMappingLocale) {
		this.wmMappingLocale = wmMappingLocale;
	}
	public String getBsmMappingLocale() {
		return bsmMappingLocale;
	}
	public void setBsmMappingLocale(String bsmMappingLocale) {
		this.bsmMappingLocale = bsmMappingLocale;
	}
	public String getMcMappingLocale() {
		return mcMappingLocale;
	}
	public void setMcMappingLocale(String mcMappingLocale) {
		this.mcMappingLocale = mcMappingLocale;
	}
	public String getAtMappingLocale() {
		return atMappingLocale;
	}
	public void setAtMappingLocale(String atMappingLocale) {
		this.atMappingLocale = atMappingLocale;
	}
	public String getMtMappingLocale() {
		return mtMappingLocale;
	}
	public void setMtMappingLocale(String mtMappingLocale) {
		this.mtMappingLocale = mtMappingLocale;
	}
	public String getEngineMappingLocale() {
		return engineMappingLocale;
	}
	public void setEngineMappingLocale(String engineMappingLocale) {
		this.engineMappingLocale = engineMappingLocale;
	}
	public String getWdMappingLocale() {
		return wdMappingLocale;
	}
	public void setWdMappingLocale(String wdMappingLocale) {
		this.wdMappingLocale = wdMappingLocale;
	}
	public String getTqgMappingLocale() {
		return tqgMappingLocale;
	}
	public void setTqgMappingLocale(String tqgMappingLocale) {
		this.tqgMappingLocale = tqgMappingLocale;
	}
}
