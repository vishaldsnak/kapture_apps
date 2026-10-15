package com.mazda.gms3.mdm.dataexttool.vo;

import java.io.File;
import java.util.ArrayList;

public class DataExportToolItemDetails {

	private long scheduleId=0;
	private long itemId=0;
	private int srNo;
	private String locale=null;
	private String model=null;
//	private String carlineCode=null;
	private String channelName=null;
	private String documentType = null;
	private long documentsCounts = 0;
	private String documentId = null;
	private boolean editableFlag=false;
	private ArrayList<String> documentIdsList = null;
	
	
	private String channelFolderPath =null;
	private String processingStatus=null;
	private String errorMessage=null;
	private File xmlFile=null;
	private String documentDirPath = null;
	
	
	private String completionStatus=null;
	private long totalDocsCount=0;
	private long successDocsCount=0;
	private long failureDocsCount=0;
	
	private ArrayList<DataExportToolItemDetails> innerLinkDocumetsList = null;
	private ArrayList<DataExportToolInnerLinkDetails> innerLinksList = null;
	
	
	private long totalInnerLinksCount=0;
	private long successInnerLinksCount=0;
	private long failureInnerLinksCount=0;
	private String innerLinksProcessingStatus=null;
	private String innerLinksCompletionStatus=null;
	
	private long totalOkAssetsCount=0;
	private long successOkAssetsCount=0;
	private long failureOkAssetsCount=0;
	
	public long getTotalOkAssetsCount() {
		return totalOkAssetsCount;
	}
	public void setTotalOkAssetsCount(long totalOkAssetsCount) {
		this.totalOkAssetsCount = totalOkAssetsCount;
	}
	public long getSuccessOkAssetsCount() {
		return successOkAssetsCount;
	}
	public void setSuccessOkAssetsCount(long successOkAssetsCount) {
		this.successOkAssetsCount = successOkAssetsCount;
	}
	public long getFailureOkAssetsCount() {
		return failureOkAssetsCount;
	}
	public void setFailureOkAssetsCount(long failureOkAssetsCount) {
		this.failureOkAssetsCount = failureOkAssetsCount;
	}
	public long getTotalInnerLinksCount() {
		return totalInnerLinksCount;
	}
	public void setTotalInnerLinksCount(long totalInnerLinksCount) {
		this.totalInnerLinksCount = totalInnerLinksCount;
	}
	public long getSuccessInnerLinksCount() {
		return successInnerLinksCount;
	}
	public void setSuccessInnerLinksCount(long successInnerLinksCount) {
		this.successInnerLinksCount = successInnerLinksCount;
	}
	public long getFailureInnerLinksCount() {
		return failureInnerLinksCount;
	}
	public void setFailureInnerLinksCount(long failureInnerLinksCount) {
		this.failureInnerLinksCount = failureInnerLinksCount;
	}
	public String getInnerLinksProcessingStatus() {
		return innerLinksProcessingStatus;
	}
	public void setInnerLinksProcessingStatus(String innerLinksProcessingStatus) {
		this.innerLinksProcessingStatus = innerLinksProcessingStatus;
	}
	public String getInnerLinksCompletionStatus() {
		return innerLinksCompletionStatus;
	}
	public void setInnerLinksCompletionStatus(String innerLinksCompletionStatus) {
		this.innerLinksCompletionStatus = innerLinksCompletionStatus;
	}
	public ArrayList<DataExportToolItemDetails> getInnerLinkDocumetsList() {
		return innerLinkDocumetsList;
	}
	public void setInnerLinkDocumetsList(ArrayList<DataExportToolItemDetails> innerLinkDocumetsList) {
		this.innerLinkDocumetsList = innerLinkDocumetsList;
	}
	public ArrayList<DataExportToolInnerLinkDetails> getInnerLinksList() {
		return innerLinksList;
	}
	public void setInnerLinksList(ArrayList<DataExportToolInnerLinkDetails> innerLinksList) {
		this.innerLinksList = innerLinksList;
	}
	public String getCompletionStatus() {
		return completionStatus;
	}
	public void setCompletionStatus(String completionStatus) {
		this.completionStatus = completionStatus;
	}
	public long getTotalDocsCount() {
		return totalDocsCount;
	}
	public void setTotalDocsCount(long totalDocsCount) {
		this.totalDocsCount = totalDocsCount;
	}
	public long getSuccessDocsCount() {
		return successDocsCount;
	}
	public void setSuccessDocsCount(long successDocsCount) {
		this.successDocsCount = successDocsCount;
	}
	public long getFailureDocsCount() {
		return failureDocsCount;
	}
	public void setFailureDocsCount(long failureDocsCount) {
		this.failureDocsCount = failureDocsCount;
	}
	public String getDocumentDirPath() {
		return documentDirPath;
	}
	public void setDocumentDirPath(String documentDirPath) {
		this.documentDirPath = documentDirPath;
	}
	public String getChannelFolderPath() {
		return channelFolderPath;
	}
	public void setChannelFolderPath(String channelFolderPath) {
		this.channelFolderPath = channelFolderPath;
	}
	public String getProcessingStatus() {
		return processingStatus;
	}
	public void setProcessingStatus(String processingStatus) {
		this.processingStatus = processingStatus;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public File getXmlFile() {
		return xmlFile;
	}
	public void setXmlFile(File xmlFile) {
		this.xmlFile = xmlFile;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
	}
	public long getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
	}
	public ArrayList<String> getDocumentIdsList() {
		return documentIdsList;
	}
	public void setDocumentIdsList(ArrayList<String> documentIdsList) {
		this.documentIdsList = documentIdsList;
	}
	public boolean isEditableFlag() {
		return editableFlag;
	}
	public void setEditableFlag(boolean editableFlag) {
		this.editableFlag = editableFlag;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
//	public String getCarlineCode() {
//		return carlineCode;
//	}
//	public void setCarlineCode(String carlineCode) {
//		this.carlineCode = carlineCode;
//	}
	public String getChannelName() {
		return channelName;
	}
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	public long getDocumentsCounts() {
		return documentsCounts;
	}
	public void setDocumentsCounts(long documentsCounts) {
		this.documentsCounts = documentsCounts;
	}
}