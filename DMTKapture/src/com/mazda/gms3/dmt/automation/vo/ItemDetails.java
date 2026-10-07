package com.mazda.gms3.dmt.automation.vo;

import java.util.ArrayList;
import java.util.Map;

public class ItemDetails {
	
	private long scheduleId;
	private long itemId;
	private int srNo;
	private String channelRefKey=null;
	private String channelLabel=null;
	
	private String documentTypeRefKey=null;
	private String documentTypeLabel=null;
	
	private String locale=null;
	private Long totalDocumentsCount=null;
	private String carlineInfo=null;
	private String displayCarlineInfo=null;
	private String remarks=null;
	
	private ArrayList<ExcelRowDetails> applicableVINList=null;
	private ArrayList<Map<String, Object>> impactedDocumentsList = null;
	
	
	private boolean rowSelected=false;
	private boolean showCheckBox=true;
	
	private Long applicableVinsCount=null;
	
	
	private Long docIdnSchId=null;
	private Long docIdnItemId=null;
	
	
	
	public Long getDocIdnSchId() {
		return docIdnSchId;
	}
	public void setDocIdnSchId(Long docIdnSchId) {
		this.docIdnSchId = docIdnSchId;
	}
	public Long getDocIdnItemId() {
		return docIdnItemId;
	}
	public void setDocIdnItemId(Long docIdnItemId) {
		this.docIdnItemId = docIdnItemId;
	}
	public Long getApplicableVinsCount() {
		return applicableVinsCount;
	}
	public void setApplicableVinsCount(Long applicableVinsCount) {
		this.applicableVinsCount = applicableVinsCount;
	}
	public long getScheduleId() {
		return scheduleId;
	}
	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
	}
	public boolean isShowCheckBox() {
		return showCheckBox;
	}
	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}
	public String getDisplayCarlineInfo() {
		return displayCarlineInfo;
	}
	public void setDisplayCarlineInfo(String displayCarlineInfo) {
		this.displayCarlineInfo = displayCarlineInfo;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
	}
	public boolean isRowSelected() {
		return rowSelected;
	}
	public void setRowSelected(boolean rowSelected) {
		this.rowSelected = rowSelected;
	}
	public ArrayList<Map<String, Object>> getImpactedDocumentsList() {
		return impactedDocumentsList;
	}
	public void setImpactedDocumentsList(
			ArrayList<Map<String, Object>> impactedDocumentsList) {
		this.impactedDocumentsList = impactedDocumentsList;
	}
	public ArrayList<ExcelRowDetails> getApplicableVINList() {
		return applicableVINList;
	}
	public void setApplicableVINList(ArrayList<ExcelRowDetails> applicableVINList) {
		this.applicableVINList = applicableVINList;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public String getChannelRefKey() {
		return channelRefKey;
	}
	public void setChannelRefKey(String channelRefKey) {
		this.channelRefKey = channelRefKey;
	}
	public String getChannelLabel() {
		return channelLabel;
	}
	public void setChannelLabel(String channelLabel) {
		this.channelLabel = channelLabel;
	}
	public String getDocumentTypeRefKey() {
		return documentTypeRefKey;
	}
	public void setDocumentTypeRefKey(String documentTypeRefKey) {
		this.documentTypeRefKey = documentTypeRefKey;
	}
	public String getDocumentTypeLabel() {
		return documentTypeLabel;
	}
	public void setDocumentTypeLabel(String documentTypeLabel) {
		this.documentTypeLabel = documentTypeLabel;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public Long getTotalDocumentsCount() {
		return totalDocumentsCount;
	}
	public void setTotalDocumentsCount(Long totalDocumentsCount) {
		this.totalDocumentsCount = totalDocumentsCount;
	}
	public String getCarlineInfo() {
		return carlineInfo;
	}
	public void setCarlineInfo(String carlineInfo) {
		this.carlineInfo = carlineInfo;
	}
}