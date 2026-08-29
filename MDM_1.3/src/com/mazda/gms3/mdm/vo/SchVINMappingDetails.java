package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;

public class SchVINMappingDetails {
	
	private int srNo;
	private long autoId;
	private long dmtScheduleId;
	private String locale=null;
	private String carlineCode=null;
	private String modelName=null;
	private String wmiCode=null;
	private String vdsCode=null;
	private String visStartRange=null;
	private String visEndRange=null;
	private String sourceFileName=null;
	private String sourceFilePath=null;
	private String title=null;
	private String documentId=null;
	

	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private boolean showCheckBox = true;
	
	private boolean saveStatusWhileImport=false;
	private String modelNotFoundMessage=null;
	private String documentNotFoundMessage=null;
	
	
	
	
	public String getDocumentNotFoundMessage() {
		return documentNotFoundMessage;
	}
	public void setDocumentNotFoundMessage(String documentNotFoundMessage) {
		this.documentNotFoundMessage = documentNotFoundMessage;
	}
	public String getModelNotFoundMessage() {
		return modelNotFoundMessage;
	}
	public void setModelNotFoundMessage(String modelNotFoundMessage) {
		this.modelNotFoundMessage = modelNotFoundMessage;
	}
	public boolean isSaveStatusWhileImport() {
		return saveStatusWhileImport;
	}
	public void setSaveStatusWhileImport(boolean saveStatusWhileImport) {
		this.saveStatusWhileImport = saveStatusWhileImport;
	}
	public long getDmtScheduleId() {
		return dmtScheduleId;
	}
	public void setDmtScheduleId(long dmtScheduleId) {
		this.dmtScheduleId = dmtScheduleId;
	}
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public long getAutoId() {
		return autoId;
	}
	public void setAutoId(long autoId) {
		this.autoId = autoId;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
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
	public String getSourceFileName() {
		return sourceFileName;
	}
	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}
	public String getSourceFilePath() {
		return sourceFilePath;
	}
	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public Timestamp getEntryTime() {
		return entryTime;
	}
	public void setEntryTime(Timestamp entryTime) {
		this.entryTime = entryTime;
	}
	public Timestamp getUpdatedTime() {
		return updatedTime;
	}
	public void setUpdatedTime(Timestamp updatedTime) {
		this.updatedTime = updatedTime;
	}
	public boolean isEditableFlag() {
		return editableFlag;
	}
	public void setEditableFlag(boolean editableFlag) {
		this.editableFlag = editableFlag;
	}
	public boolean isShowCheckBox() {
		return showCheckBox;
	}
	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}

	
	
}
