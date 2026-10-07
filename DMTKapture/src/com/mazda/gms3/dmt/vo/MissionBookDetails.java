package com.mazda.gms3.dmt.vo;

import java.sql.Timestamp;

public class MissionBookDetails {

	private int srNo;
	private Long bookId=null;
	private Long manualLanguageId=null;
	private String manualLanguageCode=null;
	private String bookCode=null;
	private String bookNameEng=null;
	private String bookNameReg=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	public int getSrNo() {
		return srNo;
	}
	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}
	public Long getBookId() {
		return bookId;
	}
	public void setBookId(Long bookId) {
		this.bookId = bookId;
	}
	public Long getManualLanguageId() {
		return manualLanguageId;
	}
	public void setManualLanguageId(Long manualLanguageId) {
		this.manualLanguageId = manualLanguageId;
	}
	public String getManualLanguageCode() {
		return manualLanguageCode;
	}
	public void setManualLanguageCode(String manualLanguageCode) {
		this.manualLanguageCode = manualLanguageCode;
	}
	public String getBookCode() {
		return bookCode;
	}
	public void setBookCode(String bookCode) {
		this.bookCode = bookCode;
	}
	public String getBookNameEng() {
		return bookNameEng;
	}
	public void setBookNameEng(String bookNameEng) {
		this.bookNameEng = bookNameEng;
	}
	public String getBookNameReg() {
		return bookNameReg;
	}
	public void setBookNameReg(String bookNameReg) {
		this.bookNameReg = bookNameReg;
	}
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	public String getFlagLabel() {
		return flagLabel;
	}
	public void setFlagLabel(String flagLabel) {
		this.flagLabel = flagLabel;
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
}