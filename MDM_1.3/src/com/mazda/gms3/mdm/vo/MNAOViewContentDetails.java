package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MNAOViewContentDetails {

	private String documentId = null;
	private String locale = null;
	private String contentId = null;
	private String recordId = null;
	private String documentType = null;
	private String documentSubType = null;
	private String documentStatus = null;
	private String sourceLocation = null;

	private String model = null;
	private String year = null;
	private String catCodeLevel1 = null;
	private String catNameLevel1 = null;
	private String catCodeLevel2 = null;
	private String catNameLevel2 = null;
	private String catCodeLevel3 = null;
	private String catNameLevel3 = null;

	private String title = null;
	private String description=null;
	private String wmiCode = null;
	private String vdsCode = null;
	private String visStartRange = null;
	private String visEndRange = null;

	private String processingStatus = null;
	private String errorCode = null;
	private String errorMessage = null;
	private Timestamp processingTime = null;
	private Timestamp publishDate = null;
	private Timestamp displayEndDate = null;

	private Timestamp documentCreateDate = null;
	private Timestamp documentLastModifiedDate = null;
	private String issueDate = null;
	private String firstPublicationDate = null;
	private String siNumber = null;
	private String documentTypeName = null;
	private String documentSubTypeName = null;

	private Timestamp imDocLastModifiedDate=null;
	private String viewContentType=null;
	private List<MNAOViewContentDetails> viewContentList = null;
	private String processingModelForMNAODataExport =null;
	
	public String getProcessingModelForMNAODataExport() {
		return processingModelForMNAODataExport;
	}

	public void setProcessingModelForMNAODataExport(String processingModelForMNAODataExport) {
		this.processingModelForMNAODataExport = processingModelForMNAODataExport;
	}

	public List<MNAOViewContentDetails> getViewContentList() {
		return viewContentList;
	}

	public void setViewContentList(List<MNAOViewContentDetails> viewContentList) {
		this.viewContentList = viewContentList;
	}

	public String getViewContentType() {
		return viewContentType;
	}

	public void setViewContentType(String viewContentType) {
		this.viewContentType = viewContentType;
	}

	public Timestamp getImDocLastModifiedDate() {
		return imDocLastModifiedDate;
	}

	public void setImDocLastModifiedDate(Timestamp imDocLastModifiedDate) {
		this.imDocLastModifiedDate = imDocLastModifiedDate;
	}

	private ArrayList<MNAOViewContentDetails> modelYearList = new ArrayList<MNAOViewContentDetails>();
	private ArrayList<MNAOViewContentDetails> vinList = new ArrayList<MNAOViewContentDetails>();
	
	
	public ArrayList<MNAOViewContentDetails> getModelYearList() {
		return modelYearList;
	}

	public void setModelYearList(ArrayList<MNAOViewContentDetails> modelYearList) {
		this.modelYearList = modelYearList;
	}

	public ArrayList<MNAOViewContentDetails> getVinList() {
		return vinList;
	}

	public void setVinList(ArrayList<MNAOViewContentDetails> vinList) {
		this.vinList = vinList;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Timestamp getDocumentCreateDate() {
		return documentCreateDate;
	}

	public void setDocumentCreateDate(Timestamp documentCreateDate) {
		this.documentCreateDate = documentCreateDate;
	}

	public Timestamp getDocumentLastModifiedDate() {
		return documentLastModifiedDate;
	}

	public void setDocumentLastModifiedDate(Timestamp documentLastModifiedDate) {
		this.documentLastModifiedDate = documentLastModifiedDate;
	}

	public String getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(String issueDate) {
		this.issueDate = issueDate;
	}

	public String getFirstPublicationDate() {
		return firstPublicationDate;
	}

	public void setFirstPublicationDate(String firstPublicationDate) {
		this.firstPublicationDate = firstPublicationDate;
	}

	public String getSiNumber() {
		return siNumber;
	}

	public void setSiNumber(String siNumber) {
		this.siNumber = siNumber;
	}

	public String getDocumentTypeName() {
		return documentTypeName;
	}

	public void setDocumentTypeName(String documentTypeName) {
		this.documentTypeName = documentTypeName;
	}

	public String getDocumentSubTypeName() {
		return documentSubTypeName;
	}

	public void setDocumentSubTypeName(String documentSubTypeName) {
		this.documentSubTypeName = documentSubTypeName;
	}

	public Timestamp getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Timestamp publishDate) {
		this.publishDate = publishDate;
	}

	public Timestamp getDisplayEndDate() {
		return displayEndDate;
	}

	public void setDisplayEndDate(Timestamp displayEndDate) {
		this.displayEndDate = displayEndDate;
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

	public Timestamp getProcessingTime() {
		return processingTime;
	}

	public void setProcessingTime(Timestamp processingTime) {
		this.processingTime = processingTime;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public String getDocumentStatus() {
		return documentStatus;
	}

	public void setDocumentStatus(String documentStatus) {
		this.documentStatus = documentStatus;
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

	public String getCatCodeLevel1() {
		return catCodeLevel1;
	}

	public void setCatCodeLevel1(String catCodeLevel1) {
		this.catCodeLevel1 = catCodeLevel1;
	}

	public String getCatNameLevel1() {
		return catNameLevel1;
	}

	public void setCatNameLevel1(String catNameLevel1) {
		this.catNameLevel1 = catNameLevel1;
	}

	public String getCatCodeLevel2() {
		return catCodeLevel2;
	}

	public void setCatCodeLevel2(String catCodeLevel2) {
		this.catCodeLevel2 = catCodeLevel2;
	}

	public String getCatNameLevel2() {
		return catNameLevel2;
	}

	public void setCatNameLevel2(String catNameLevel2) {
		this.catNameLevel2 = catNameLevel2;
	}

	public String getCatCodeLevel3() {
		return catCodeLevel3;
	}

	public void setCatCodeLevel3(String catCodeLevel3) {
		this.catCodeLevel3 = catCodeLevel3;
	}

	public String getCatNameLevel3() {
		return catNameLevel3;
	}

	public void setCatNameLevel3(String catNameLevel3) {
		this.catNameLevel3 = catNameLevel3;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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

	public String getSourceLocation() {
		return sourceLocation;
	}

	public void setSourceLocation(String sourceLocation) {
		this.sourceLocation = sourceLocation;
	}

	public String getDocumentSubType() {
		return documentSubType;
	}

	public void setDocumentSubType(String documentSubType) {
		this.documentSubType = documentSubType;
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}
}
