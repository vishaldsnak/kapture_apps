package com.mazda.gms3.dmt.vo;

import java.sql.Timestamp;

public class MNAOModelYearViewContentDetails {

	private String sourceFilePath = null;
	private String locale = null;
	private String documentId = null;
	private String model = null;
	private String year = null;
	private String wmiCode = null;
	private String vdsCode = null;
	private String visStartRange = null;
	private String visEndRange = null;
	private String esiCatLevel1Code = null;
	private String esiCatLevel2Code = null;
	private String esiCatLevel3Code = null;
	private String esiCatLevel1Name = null;
	private String esiCatLevel2Name = null;
	private String esiCatLevel3Name = null;

	private String manualType = null;
	private String manualTypeLabel = null;
	private Timestamp imDocCreateDate = null;
	private Timestamp imDocModifiedDate = null;

	private String title = null;
	private String description = null;

	/** the d01.txt (...) row this view content row is written for - its display order names and sequence */
	private com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails displayOrder = null;

	public com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails displayOrder) {
		this.displayOrder = displayOrder;
	}

	private String processingStatus = null;
	private String errorCodes=null;
	private String errorMessages=null;

	
	
	public String getErrorCodes() {
		return errorCodes;
	}

	public void setErrorCodes(String errorCodes) {
		this.errorCodes = errorCodes;
	}

	public String getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessages(String errorMessages) {
		this.errorMessages = errorMessages;
	}

	public String getProcessingStatus() {
		return processingStatus;
	}

	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
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

	public String getEsiCatLevel1Code() {
		return esiCatLevel1Code;
	}

	public void setEsiCatLevel1Code(String esiCatLevel1Code) {
		this.esiCatLevel1Code = esiCatLevel1Code;
	}

	public String getEsiCatLevel2Code() {
		return esiCatLevel2Code;
	}

	public void setEsiCatLevel2Code(String esiCatLevel2Code) {
		this.esiCatLevel2Code = esiCatLevel2Code;
	}

	public String getEsiCatLevel3Code() {
		return esiCatLevel3Code;
	}

	public void setEsiCatLevel3Code(String esiCatLevel3Code) {
		this.esiCatLevel3Code = esiCatLevel3Code;
	}

	public String getEsiCatLevel1Name() {
		return esiCatLevel1Name;
	}

	public void setEsiCatLevel1Name(String esiCatLevel1Name) {
		this.esiCatLevel1Name = esiCatLevel1Name;
	}

	public String getEsiCatLevel2Name() {
		return esiCatLevel2Name;
	}

	public void setEsiCatLevel2Name(String esiCatLevel2Name) {
		this.esiCatLevel2Name = esiCatLevel2Name;
	}

	public String getEsiCatLevel3Name() {
		return esiCatLevel3Name;
	}

	public void setEsiCatLevel3Name(String esiCatLevel3Name) {
		this.esiCatLevel3Name = esiCatLevel3Name;
	}

	public String getManualType() {
		return manualType;
	}

	public void setManualType(String manualType) {
		this.manualType = manualType;
	}

	public String getManualTypeLabel() {
		return manualTypeLabel;
	}

	public void setManualTypeLabel(String manualTypeLabel) {
		this.manualTypeLabel = manualTypeLabel;
	}

	public Timestamp getImDocCreateDate() {
		return imDocCreateDate;
	}

	public void setImDocCreateDate(Timestamp imDocCreateDate) {
		this.imDocCreateDate = imDocCreateDate;
	}

	public Timestamp getImDocModifiedDate() {
		return imDocModifiedDate;
	}

	public void setImDocModifiedDate(Timestamp imDocModifiedDate) {
		this.imDocModifiedDate = imDocModifiedDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSourceFilePath() {
		return sourceFilePath;
	}

	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}
}
