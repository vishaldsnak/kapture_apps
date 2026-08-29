package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.RumVinDetails;

public class RumVinDataBean {

	private ArrayList<RumVinDetails> dataList = new ArrayList<RumVinDetails>();
	private boolean showReadControls = false;
	private boolean showWriteControls = false;
	
	private int offset = 0;
	private int totalPages = 0;
	private int currentPageNo = 0;

	
	private boolean navigationBlock = false;
	private boolean firstDisbled = false;
	private boolean lastDisabled = false;
	private boolean previousDisabled = false;
	private boolean nextDisabled = false;
	private String jspData = null;
	
	private ArrayList<RumVinDetails> displayDataList = new ArrayList<RumVinDetails>();
	
	
	public ArrayList<RumVinDetails> getDisplayDataList() {
		return displayDataList;
	}
	public void setDisplayDataList(ArrayList<RumVinDetails> displayDataList) {
		this.displayDataList = displayDataList;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public int getTotalPages() {
		return totalPages;
	}
	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}
	public int getCurrentPageNo() {
		return currentPageNo;
	}
	public void setCurrentPageNo(int currentPageNo) {
		this.currentPageNo = currentPageNo;
	}
	public boolean isNavigationBlock() {
		return navigationBlock;
	}
	public void setNavigationBlock(boolean navigationBlock) {
		this.navigationBlock = navigationBlock;
	}
	public boolean isFirstDisbled() {
		return firstDisbled;
	}
	public void setFirstDisbled(boolean firstDisbled) {
		this.firstDisbled = firstDisbled;
	}
	public boolean isLastDisabled() {
		return lastDisabled;
	}
	public void setLastDisabled(boolean lastDisabled) {
		this.lastDisabled = lastDisabled;
	}
	public boolean isPreviousDisabled() {
		return previousDisabled;
	}
	public void setPreviousDisabled(boolean previousDisabled) {
		this.previousDisabled = previousDisabled;
	}
	public boolean isNextDisabled() {
		return nextDisabled;
	}
	public void setNextDisabled(boolean nextDisabled) {
		this.nextDisabled = nextDisabled;
	}
	public String getJspData() {
		return jspData;
	}
	public void setJspData(String jspData) {
		this.jspData = jspData;
	}
	public ArrayList<RumVinDetails> getDataList() {
		return dataList;
	}
	public void setDataList(ArrayList<RumVinDetails> dataList) {
		this.dataList = dataList;
	}
	public boolean isShowReadControls() {
		return showReadControls;
	}
	public void setShowReadControls(boolean showReadControls) {
		this.showReadControls = showReadControls;
	}
	public boolean isShowWriteControls() {
		return showWriteControls;
	}
	public void setShowWriteControls(boolean showWriteControls) {
		this.showWriteControls = showWriteControls;
	}
	
	
}
