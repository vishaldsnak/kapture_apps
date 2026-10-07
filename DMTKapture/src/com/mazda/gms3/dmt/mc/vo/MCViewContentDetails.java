package com.mazda.gms3.dmt.mc.vo;

import java.sql.Timestamp;

public class MCViewContentDetails {

	private String sourceFilePath=null;
	private String locale=null;
	private String documentId=null;
	private String modelType=null;
	private String carlineCode=null;
	private String carlineNameEng=null;
	private String carlineNameReg=null;
	private String wmiCode=null;
	private String vdsCode=null;
	private String visStartRange=null;
	private String visEndRange=null;
	private String esiCatLevel1Code=null;
	private String esiCatLevel2Code=null;
	private String esiCatLevel3Code=null;
	private String esiCatLevel1Name=null;
	private String esiCatLevel2Name=null;
	private String esiCatLevel3Name=null;
	
	private String engineBookCode=null;
	private String engineBookName=null;
	private String missionBookCode=null;
	private String missionBookName=null;
	private String displayOrderCodeLevel1=null;
	private String displayOrderCodeLevel2=null;
	private String displayOrderCodeLevel3=null;
	private String displayOrderCodeLevel4=null;
	private String displayOrderCodeLevel5=null;
	private String displayOrderCodeLevel6=null;
	
	private String displayOrderNameLevel1=null;
	private String displayOrderNameLevel2=null;
	private String displayOrderNameLevel3=null;
	private String displayOrderNameLevel4=null;
	private String displayOrderNameLevel5=null;
	private String displayOrderNameLevel6=null;

	private String displayOrderSequenceNo=null;
	
	private String manualType=null;
	private String manualTypeLabel=null;
	private Timestamp imDocCreateDate=null;
	private Timestamp imDocModifiedDate=null;
	
	private String title=null;
	private String description=null;
	
	private String processingStatus=null;
	private String remarks=null;
	
	private String esiCatFlag=null;
	
	private String engineType=null;
	private String missionType=null;
	
	public String getEngineType() {
		return engineType;
	}
	public void setEngineType(String engineType) {
		this.engineType = engineType;
	}
	public String getMissionType() {
		return missionType;
	}
	public void setMissionType(String missionType) {
		this.missionType = missionType;
	}
	public String getEsiCatFlag() {
		return esiCatFlag;
	}
	public void setEsiCatFlag(String esiCatFlag) {
		this.esiCatFlag = esiCatFlag;
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
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getCarlineNameEng() {
		return carlineNameEng;
	}
	public void setCarlineNameEng(String carlineNameEng) {
		this.carlineNameEng = carlineNameEng;
	}
	public String getCarlineNameReg() {
		return carlineNameReg;
	}
	public void setCarlineNameReg(String carlineNameReg) {
		this.carlineNameReg = carlineNameReg;
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
	public String getEngineBookCode() {
		return engineBookCode;
	}
	public void setEngineBookCode(String engineBookCode) {
		this.engineBookCode = engineBookCode;
	}
	public String getEngineBookName() {
		return engineBookName;
	}
	public void setEngineBookName(String engineBookName) {
		this.engineBookName = engineBookName;
	}
	public String getMissionBookCode() {
		return missionBookCode;
	}
	public void setMissionBookCode(String missionBookCode) {
		this.missionBookCode = missionBookCode;
	}
	public String getMissionBookName() {
		return missionBookName;
	}
	public void setMissionBookName(String missionBookName) {
		this.missionBookName = missionBookName;
	}
	public String getDisplayOrderCodeLevel1() {
		return displayOrderCodeLevel1;
	}
	public void setDisplayOrderCodeLevel1(String displayOrderCodeLevel1) {
		this.displayOrderCodeLevel1 = displayOrderCodeLevel1;
	}
	public String getDisplayOrderCodeLevel2() {
		return displayOrderCodeLevel2;
	}
	public void setDisplayOrderCodeLevel2(String displayOrderCodeLevel2) {
		this.displayOrderCodeLevel2 = displayOrderCodeLevel2;
	}
	public String getDisplayOrderCodeLevel3() {
		return displayOrderCodeLevel3;
	}
	public void setDisplayOrderCodeLevel3(String displayOrderCodeLevel3) {
		this.displayOrderCodeLevel3 = displayOrderCodeLevel3;
	}
	public String getDisplayOrderCodeLevel4() {
		return displayOrderCodeLevel4;
	}
	public void setDisplayOrderCodeLevel4(String displayOrderCodeLevel4) {
		this.displayOrderCodeLevel4 = displayOrderCodeLevel4;
	}
	public String getDisplayOrderCodeLevel5() {
		return displayOrderCodeLevel5;
	}
	public void setDisplayOrderCodeLevel5(String displayOrderCodeLevel5) {
		this.displayOrderCodeLevel5 = displayOrderCodeLevel5;
	}
	public String getDisplayOrderCodeLevel6() {
		return displayOrderCodeLevel6;
	}
	public void setDisplayOrderCodeLevel6(String displayOrderCodeLevel6) {
		this.displayOrderCodeLevel6 = displayOrderCodeLevel6;
	}
	public String getDisplayOrderNameLevel1() {
		return displayOrderNameLevel1;
	}
	public void setDisplayOrderNameLevel1(String displayOrderNameLevel1) {
		this.displayOrderNameLevel1 = displayOrderNameLevel1;
	}
	public String getDisplayOrderNameLevel2() {
		return displayOrderNameLevel2;
	}
	public void setDisplayOrderNameLevel2(String displayOrderNameLevel2) {
		this.displayOrderNameLevel2 = displayOrderNameLevel2;
	}
	public String getDisplayOrderNameLevel3() {
		return displayOrderNameLevel3;
	}
	public void setDisplayOrderNameLevel3(String displayOrderNameLevel3) {
		this.displayOrderNameLevel3 = displayOrderNameLevel3;
	}
	public String getDisplayOrderNameLevel4() {
		return displayOrderNameLevel4;
	}
	public void setDisplayOrderNameLevel4(String displayOrderNameLevel4) {
		this.displayOrderNameLevel4 = displayOrderNameLevel4;
	}
	public String getDisplayOrderNameLevel5() {
		return displayOrderNameLevel5;
	}
	public void setDisplayOrderNameLevel5(String displayOrderNameLevel5) {
		this.displayOrderNameLevel5 = displayOrderNameLevel5;
	}
	public String getDisplayOrderNameLevel6() {
		return displayOrderNameLevel6;
	}
	public void setDisplayOrderNameLevel6(String displayOrderNameLevel6) {
		this.displayOrderNameLevel6 = displayOrderNameLevel6;
	}
	public String getDisplayOrderSequenceNo() {
		return displayOrderSequenceNo;
	}
	public void setDisplayOrderSequenceNo(String displayOrderSequenceNo) {
		this.displayOrderSequenceNo = displayOrderSequenceNo;
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
	public String getModelType() {
		return modelType;
	}
	public void setModelType(String modelType) {
		this.modelType = modelType;
	}
	public String getSourceFilePath() {
		return sourceFilePath;
	}
	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}
}