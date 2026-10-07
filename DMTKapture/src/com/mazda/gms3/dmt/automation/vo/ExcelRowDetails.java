package com.mazda.gms3.dmt.automation.vo;

import com.mazda.gms3.dmt.vo.VinMLMappingDetails;

public class ExcelRowDetails {

	private int rowNo;
	private String locale=null;
	private String oldCarlineCode=null;
	private String oldWmiCode=null;
	private String oldVdsCode=null;
	private String oldVisStartRange=null;
	private String oldVisEndRange=null;
	
	private String oldRefKey=null;
	
	private String newCarlineCode=null;
	private String newWmiCode=null;
	private String newVdsCode=null;
	private String newVisStartRange=null;
	private String newVisEndRange=null;
	
	private String newRefKey=null;
	
	// used for MNAO LOCALES - MODEL_YEAR REF KEY
	private String oldModelName=null;
	
	private String newModelName=null;
	
	private boolean localeExistsInMDM=false;
	private boolean localeExistsInIM=false;
	
	private boolean oldVinExistsinMDM=false;
	private boolean oldVinExistsinIM=false;

	private boolean newVinExistsinMDM=false;
	private boolean newVinExistsinIM=false;
	
	
	private VinMLMappingDetails oldDetails=null;
	private VinMLMappingDetails newDetails=null;
	private String processingStatus=null;
	private String remarks=null;
	
	/*
	 * REQUIRED FOR ENGLIGH MODLES NAME FOR ALL MARKET LOCALES 
	 * TO BE USED WHILE IDENTIFYING IMPACTED DOCUMENTS
	 *  AND WHILE POPULATING VIEW CONTENT DATA
	 * DATE 24 JULY 2019 
	 */
	private String oldModelNameEngForMC=null;
	private String oldModelNameRegForMNAOMME=null;
	private String newModelNameEngForMC=null;
	private String newModelNameRegForMNAOMME=null;
	
	private Long scheduleId=null;
	private Long itemId=null;
	
	private String documentId=null;
	private String docTypeRefKey=null;
	private String recordType=null;
	
	public String getRecordType() {
		return recordType;
	}
	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getDocTypeRefKey() {
		return docTypeRefKey;
	}
	public void setDocTypeRefKey(String docTypeRefKey) {
		this.docTypeRefKey = docTypeRefKey;
	}
	public Long getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(Long scheduleId) {
		this.scheduleId = scheduleId;
	}
	public Long getItemId() {
		return itemId;
	}
	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}
	public String getOldModelNameEngForMC() {
		return oldModelNameEngForMC;
	}
	public void setOldModelNameEngForMC(String oldModelNameEngForMC) {
		this.oldModelNameEngForMC = oldModelNameEngForMC;
	}
	public String getOldModelNameRegForMNAOMME() {
		return oldModelNameRegForMNAOMME;
	}
	public void setOldModelNameRegForMNAOMME(String oldModelNameRegForMNAOMME) {
		this.oldModelNameRegForMNAOMME = oldModelNameRegForMNAOMME;
	}
	public String getNewModelNameEngForMC() {
		return newModelNameEngForMC;
	}
	public void setNewModelNameEngForMC(String newModelNameEngForMC) {
		this.newModelNameEngForMC = newModelNameEngForMC;
	}
	public String getNewModelNameRegForMNAOMME() {
		return newModelNameRegForMNAOMME;
	}
	public void setNewModelNameRegForMNAOMME(String newModelNameRegForMNAOMME) {
		this.newModelNameRegForMNAOMME = newModelNameRegForMNAOMME;
	}
	public VinMLMappingDetails getOldDetails() {
		return oldDetails;
	}
	public void setOldDetails(VinMLMappingDetails oldDetails) {
		this.oldDetails = oldDetails;
	}
	public VinMLMappingDetails getNewDetails() {
		return newDetails;
	}
	public void setNewDetails(VinMLMappingDetails newDetails) {
		this.newDetails = newDetails;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	public int getRowNo() {
		return rowNo;
	}
	public void setRowNo(int rowNo) {
		this.rowNo = rowNo;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
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
	public String getOldRefKey() {
		return oldRefKey;
	}
	public void setOldRefKey(String oldRefKey) {
		this.oldRefKey = oldRefKey;
	}
	public String getNewCarlineCode() {
		return newCarlineCode;
	}
	public void setNewCarlineCode(String newCarlineCode) {
		this.newCarlineCode = newCarlineCode;
	}
	public String getNewWmiCode() {
		return newWmiCode;
	}
	public void setNewWmiCode(String newWmiCode) {
		this.newWmiCode = newWmiCode;
	}
	public String getNewVdsCode() {
		return newVdsCode;
	}
	public void setNewVdsCode(String newVdsCode) {
		this.newVdsCode = newVdsCode;
	}
	public String getNewVisStartRange() {
		return newVisStartRange;
	}
	public void setNewVisStartRange(String newVisStartRange) {
		this.newVisStartRange = newVisStartRange;
	}
	public String getNewVisEndRange() {
		return newVisEndRange;
	}
	public void setNewVisEndRange(String newVisEndRange) {
		this.newVisEndRange = newVisEndRange;
	}
	public String getNewRefKey() {
		return newRefKey;
	}
	public void setNewRefKey(String newRefKey) {
		this.newRefKey = newRefKey;
	}
	public String getOldModelName() {
		return oldModelName;
	}
	public void setOldModelName(String oldModelName) {
		this.oldModelName = oldModelName;
	}
	public String getNewModelName() {
		return newModelName;
	}
	public void setNewModelName(String newModelName) {
		this.newModelName = newModelName;
	}
	public boolean isLocaleExistsInMDM() {
		return localeExistsInMDM;
	}
	public void setLocaleExistsInMDM(boolean localeExistsInMDM) {
		this.localeExistsInMDM = localeExistsInMDM;
	}
	public boolean isLocaleExistsInIM() {
		return localeExistsInIM;
	}
	public void setLocaleExistsInIM(boolean localeExistsInIM) {
		this.localeExistsInIM = localeExistsInIM;
	}
	public boolean isOldVinExistsinMDM() {
		return oldVinExistsinMDM;
	}
	public void setOldVinExistsinMDM(boolean oldVinExistsinMDM) {
		this.oldVinExistsinMDM = oldVinExistsinMDM;
	}
	public boolean isOldVinExistsinIM() {
		return oldVinExistsinIM;
	}
	public void setOldVinExistsinIM(boolean oldVinExistsinIM) {
		this.oldVinExistsinIM = oldVinExistsinIM;
	}
	public boolean isNewVinExistsinMDM() {
		return newVinExistsinMDM;
	}
	public void setNewVinExistsinMDM(boolean newVinExistsinMDM) {
		this.newVinExistsinMDM = newVinExistsinMDM;
	}
	public boolean isNewVinExistsinIM() {
		return newVinExistsinIM;
	}
	public void setNewVinExistsinIM(boolean newVinExistsinIM) {
		this.newVinExistsinIM = newVinExistsinIM;
	}
}