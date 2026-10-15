package com.mazda.gms3.mdm.nmdtool.vo;

import java.sql.Timestamp;

public class NMDScheduleDetails {

	private int srNo;
	private long scheduleId;
	private String locale=null;
	private String model=null;
	private String dayOfMonth=null;
	private String timeofMonth=null;
	private String wslId=null;
	private Timestamp lastExecutionTime=null;
	
	private Timestamp createdTime=null;
	private Timestamp modifiedTime=null;
	private String flag=null;
	private boolean selected=false;
	private String systemErrorComments=null;
	private String systemErrorMessage=null;
	
	
	public String getSystemErrorComments() {
		return systemErrorComments;
	}

	public void setSystemErrorComments(String systemErrorComments) {
		this.systemErrorComments = systemErrorComments;
	}

	public String getSystemErrorMessage() {
		return systemErrorMessage;
	}

	public void setSystemErrorMessage(String systemErrorMessage) {
		this.systemErrorMessage = systemErrorMessage;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public Timestamp getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Timestamp createdTime) {
		this.createdTime = createdTime;
	}

	public long getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(long scheduleId) {
		this.scheduleId = scheduleId;
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

	public String getDayOfMonth() {
		return dayOfMonth;
	}

	public void setDayOfMonth(String dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}

	public String getTimeofMonth() {
		return timeofMonth;
	}

	public void setTimeofMonth(String timeofMonth) {
		this.timeofMonth = timeofMonth;
	}

	public String getWslId() {
		return wslId;
	}

	public void setWslId(String wslId) {
		this.wslId = wslId;
	}

	public Timestamp getLastExecutionTime() {
		return lastExecutionTime;
	}

	public void setLastExecutionTime(Timestamp lastExecutionTime) {
		this.lastExecutionTime = lastExecutionTime;
	}

	public Timestamp getModifiedTime() {
		return modifiedTime;
	}

	public void setModifiedTime(Timestamp modifiedTime) {
		this.modifiedTime = modifiedTime;
	}
}
