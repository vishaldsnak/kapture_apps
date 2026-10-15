package com.mazda.gms3.mdm.sidataload.vo;

import java.sql.Timestamp;

public class SIChannelScheduleDetails {

	private int srNo;
	private long scheduleId;
	private String scheduleName=null;
	private String documentId=null;
	private long countryLocaleId;
	private long manualLanguageId;
	private String locale=null;
	private String fetchedVersion=null;
	private String modifiedVersion=null;
	private String documentStatus=null;
	private String wslId=null;
	private Timestamp scheduleTime=null;
	private Timestamp finishTime=null;
	private String zipFilePath=null;
	private String scheduleStatus=null;
	private String errorsShort=null;
	private String errorsComplete=null;
	private long okAssetsTotalCount;
	private long okAssetsSuccessCount;
	private long okAssetsFailureCount;
	
	private String documentType=null;
	private String reportsPath = null;
	
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public String getReportsPath() {
		return reportsPath;
	}
	public void setReportsPath(String reportsPath) {
		this.reportsPath = reportsPath;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	public long getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
	}
	public String getScheduleName() {
		return scheduleName;
	}
	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public long getCountryLocaleId() {
		return countryLocaleId;
	}
	public void setCountryLocaleId(long countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
	}
	public long getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(long manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
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
	public String getDocumentStatus() {
		return documentStatus;
	}
	public void setDocumentStatus(String documentStatus) {
		this.documentStatus = documentStatus;
	}
	public String getWslId() {
		return wslId;
	}
	public void setWslId(String wslId) {
		this.wslId = wslId;
	}
	public Timestamp getScheduleTime() {
		return scheduleTime;
	}
	public void setScheduleTime(Timestamp scheduleTime) {
		this.scheduleTime = scheduleTime;
	}
	public Timestamp getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(Timestamp finishTime) {
		this.finishTime = finishTime;
	}
	public String getZipFilePath() {
		return zipFilePath;
	}
	public void setZipFilePath(String zipFilePath) {
		this.zipFilePath = zipFilePath;
	}
	public String getScheduleStatus() {
		return scheduleStatus;
	}
	public void setScheduleStatus(String scheduleStatus) {
		this.scheduleStatus = scheduleStatus;
	}
	public String getErrorsShort() {
		return errorsShort;
	}
	public void setErrorsShort(String errorsShort) {
		this.errorsShort = errorsShort;
	}
	public String getErrorsComplete() {
		return errorsComplete;
	}
	public void setErrorsComplete(String errorsComplete) {
		this.errorsComplete = errorsComplete;
	}
	public long getOkAssetsTotalCount() {
		return okAssetsTotalCount;
	}
	public void setOkAssetsTotalCount(long okAssetsTotalCount) {
		this.okAssetsTotalCount = okAssetsTotalCount;
	}
	public long getOkAssetsSuccessCount() {
		return okAssetsSuccessCount;
	}
	public void setOkAssetsSuccessCount(long okAssetsSuccessCount) {
		this.okAssetsSuccessCount = okAssetsSuccessCount;
	}
	public long getOkAssetsFailureCount() {
		return okAssetsFailureCount;
	}
	public void setOkAssetsFailureCount(long okAssetsFailureCount) {
		this.okAssetsFailureCount = okAssetsFailureCount;
	}
}