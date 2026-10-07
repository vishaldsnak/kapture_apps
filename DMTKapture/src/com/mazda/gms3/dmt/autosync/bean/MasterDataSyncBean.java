package com.mazda.gms3.dmt.autosync.bean;

import java.util.ArrayList;

import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleDetails;
import com.mazda.gms3.dmt.autosync.vo.AutoSyncScheduleItemDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;

public class MasterDataSyncBean {
	
	private ArrayList<SelectItemDetails> marketList = null;
	private String marketId=null;
	
	private ArrayList<SelectItemDetails> categoryTypesList = null;
	private String selectedCategories=null;
	
	private ArrayList<AutoSyncScheduleItemDetails> itemsList = null;
	
	private ArrayList<AutoSyncScheduleDetails> transactionsList = null;
	
	private String actionClicked=null;
	private String errorMessage=null;
	private String successMessage=null;
	
	private String displayPageLength=null;
	private String displayPageNo=null;
	private String displayPageTempLength=null;
	private String displayPageTempNo=null;
	
	private boolean reloadJSP = false;
	private boolean showItemsBlock=false;
	
	
	public boolean isShowItemsBlock() {
		return showItemsBlock;
	}

	public void setShowItemsBlock(boolean showItemsBlock) {
		this.showItemsBlock = showItemsBlock;
	}

	public boolean isReloadJSP() {
		return reloadJSP;
	}

	public void setReloadJSP(boolean reloadJSP) {
		this.reloadJSP = reloadJSP;
	}

	public String getDisplayPageLength() {
		return displayPageLength;
	}

	public void setDisplayPageLength(String displayPageLength) {
		this.displayPageLength = displayPageLength;
	}

	public String getDisplayPageNo() {
		return displayPageNo;
	}

	public void setDisplayPageNo(String displayPageNo) {
		this.displayPageNo = displayPageNo;
	}

	public String getDisplayPageTempLength() {
		return displayPageTempLength;
	}

	public void setDisplayPageTempLength(String displayPageTempLength) {
		this.displayPageTempLength = displayPageTempLength;
	}

	public String getDisplayPageTempNo() {
		return displayPageTempNo;
	}

	public void setDisplayPageTempNo(String displayPageTempNo) {
		this.displayPageTempNo = displayPageTempNo;
	}

	public String getActionClicked() {
		return actionClicked;
	}

	public void setActionClicked(String actionClicked) {
		this.actionClicked = actionClicked;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getSuccessMessage() {
		return successMessage;
	}

	public void setSuccessMessage(String successMessage) {
		this.successMessage = successMessage;
	}

	public ArrayList<SelectItemDetails> getMarketList() {
		return marketList;
	}

	public void setMarketList(ArrayList<SelectItemDetails> marketList) {
		this.marketList = marketList;
	}

	public String getMarketId() {
		return marketId;
	}

	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	public ArrayList<SelectItemDetails> getCategoryTypesList() {
		return categoryTypesList;
	}

	public void setCategoryTypesList(ArrayList<SelectItemDetails> categoryTypesList) {
		this.categoryTypesList = categoryTypesList;
	}

	public String getSelectedCategories() {
		return selectedCategories;
	}

	public void setSelectedCategories(String selectedCategories) {
		this.selectedCategories = selectedCategories;
	}

	public ArrayList<AutoSyncScheduleItemDetails> getItemsList() {
		return itemsList;
	}

	public void setItemsList(ArrayList<AutoSyncScheduleItemDetails> itemsList) {
		this.itemsList = itemsList;
	}

	public ArrayList<AutoSyncScheduleDetails> getTransactionsList() {
		return transactionsList;
	}

	public void setTransactionsList(
			ArrayList<AutoSyncScheduleDetails> transactionsList) {
		this.transactionsList = transactionsList;
	}
}
