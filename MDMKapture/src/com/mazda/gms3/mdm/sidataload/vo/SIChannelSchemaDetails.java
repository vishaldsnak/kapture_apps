package com.mazda.gms3.mdm.sidataload.vo;

import java.util.List;

public class SIChannelSchemaDetails {

	private String title=null;
	private String tsbNumber=null;
	private String description=null;
	private String repairProcedure=null;
	private String callibration=null;
	private String partsInformation=null;
	private String warrantyInformation=null;
	private List<SIChannelImageDetails> imagesList= null;

	private String locale=null;
	private String wslId=null;
	private String documentId=null;
	private String contentId=null;
	private String fetchedVersion=null;
	private String modifiedVersion=null;
	private String publishStatus=null;

	private String processingStatus=null;

	private List<SIChannelErrorsDetails> errorsList = null;

	private String documentType=null;
	/*
	 * OTHER SHCMEA FIELDS
	 */
	private String campaignNumber=null;
	private String tiNumber=null;
	private String mtipsNumber=null;
	private String legacyId=null;
	private String tsbIssueDate=null;
	private String firstPublicationDate=null;
	private String siHistory=null;
	private String showAsNewsForWeek = null;
	private String mcAuthorizationNo=null;
	private String bulletinNotes=null;
	private String internalDocRefNumber = null;


	private String market=null;


	public String getMarket() {
		return market;
	}
	public void setMarket(String market) {
		this.market = market;
	}
	public String getBulletinNotes() {
		return bulletinNotes;
	}
	public void setBulletinNotes(String bulletinNotes) {
		this.bulletinNotes = bulletinNotes;
	}
	public String getInternalDocRefNumber() {
		return internalDocRefNumber;
	}
	public void setInternalDocRefNumber(String internalDocRefNumber) {
		this.internalDocRefNumber = internalDocRefNumber;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	public String getTiNumber() {
		return tiNumber;
	}
	public void setTiNumber(String tiNumber) {
		this.tiNumber = tiNumber;
	}
	public String getMtipsNumber() {
		return mtipsNumber;
	}
	public void setMtipsNumber(String mtipsNumber) {
		this.mtipsNumber = mtipsNumber;
	}
	public String getLegacyId() {
		return legacyId;
	}
	public void setLegacyId(String legacyId) {
		this.legacyId = legacyId;
	}
	public String getTsbIssueDate() {
		return tsbIssueDate;
	}
	public void setTsbIssueDate(String tsbIssueDate) {
		this.tsbIssueDate = tsbIssueDate;
	}
	public String getFirstPublicationDate() {
		return firstPublicationDate;
	}
	public void setFirstPublicationDate(String firstPublicationDate) {
		this.firstPublicationDate = firstPublicationDate;
	}
	public String getSiHistory() {
		return siHistory;
	}
	public void setSiHistory(String siHistory) {
		this.siHistory = siHistory;
	}
	public String getShowAsNewsForWeek() {
		return showAsNewsForWeek;
	}
	public void setShowAsNewsForWeek(String showAsNewsForWeek) {
		this.showAsNewsForWeek = showAsNewsForWeek;
	}
	public String getMcAuthorizationNo() {
		return mcAuthorizationNo;
	}
	public void setMcAuthorizationNo(String mcAuthorizationNo) {
		this.mcAuthorizationNo = mcAuthorizationNo;
	}
	public String getCampaignNumber() {
		return campaignNumber;
	}
	public void setCampaignNumber(String campaignNumber) {
		this.campaignNumber = campaignNumber;
	}
	public String getWslId() {
		return wslId;
	}
	public void setWslId(String wslId) {
		this.wslId = wslId;
	}
	public List<SIChannelErrorsDetails> getErrorsList() {
		return errorsList;
	}
	public void setErrorsList(List<SIChannelErrorsDetails> errorsList) {
		this.errorsList = errorsList;
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
	public String getContentId() {
		return contentId;
	}
	public void setContentId(String contentId) {
		this.contentId = contentId;
	}
	public String getFetchedVersion() {
		return fetchedVersion;
	}
	public void setFetchedVersion(String fetchedVersion) {
		this.fetchedVersion = fetchedVersion;
	}
	public String getModifiedVersion() {
		return modifiedVersion;
	}
	public void setModifiedVersion(String modifiedVersion) {
		this.modifiedVersion = modifiedVersion;
	}
	public String getPublishStatus() {
		return publishStatus;
	}
	public void setPublishStatus(String publishStatus) {
		this.publishStatus = publishStatus;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
	public List<SIChannelImageDetails> getImagesList() {
		return imagesList;
	}
	public void setImagesList(List<SIChannelImageDetails> imagesList) {
		this.imagesList = imagesList;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTsbNumber() {
		return tsbNumber;
	}
	public void setTsbNumber(String tsbNumber) {
		this.tsbNumber = tsbNumber;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getRepairProcedure() {
		return repairProcedure;
	}
	public void setRepairProcedure(String repairProcedure) {
		this.repairProcedure = repairProcedure;
	}
	public String getCallibration() {
		return callibration;
	}
	public void setCallibration(String callibration) {
		this.callibration = callibration;
	}
	public String getPartsInformation() {
		return partsInformation;
	}
	public void setPartsInformation(String partsInformation) {
		this.partsInformation = partsInformation;
	}
	public String getWarrantyInformation() {
		return warrantyInformation;
	}
	public void setWarrantyInformation(String warrantyInformation) {
		this.warrantyInformation = warrantyInformation;
	}


}
