package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;
import java.util.ArrayList;

public class MasterDataLocaleMappingDetails {
	
	private int srNo;
	private Long mappingId=null;
	private String market=null;
	private String locale=null;
	private String alternateLocale=null;
	private String masterDataType=null;
	private String masterDataTypeLabel=null;
	
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private boolean showCheckBox = true;
	
	private boolean saveStatusWhileImport=false;

	private ArrayList<SelectItemDetails> masterDataTypeList = null;
	
	
	
	public ArrayList<SelectItemDetails> getMasterDataTypeList() {
		return masterDataTypeList;
	}

	public void setMasterDataTypeList(
			ArrayList<SelectItemDetails> masterDataTypeList) {
		this.masterDataTypeList = masterDataTypeList;
	}

	public boolean isShowCheckBox() {
		return showCheckBox;
	}

	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}

	public int getSrNo() {
		return srNo;
	}

	public void setSrNo(int srNo) {
		this.srNo = srNo;
	}

	public Long getMappingId() {
		return mappingId;
	}

	public void setMappingId(Long mappingId) {
		this.mappingId = mappingId;
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getAlternateLocale() {
		return alternateLocale;
	}

	public void setAlternateLocale(String alternateLocale) {
		this.alternateLocale = alternateLocale;
	}

	public String getMasterDataType() {
		return masterDataType;
	}

	public void setMasterDataType(String masterDataType) {
		this.masterDataType = masterDataType;
	}

	public String getMasterDataTypeLabel() {
		return masterDataTypeLabel;
	}

	public void setMasterDataTypeLabel(String masterDataTypeLabel) {
		this.masterDataTypeLabel = masterDataTypeLabel;
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

	public boolean isSaveStatusWhileImport() {
		return saveStatusWhileImport;
	}

	public void setSaveStatusWhileImport(boolean saveStatusWhileImport) {
		this.saveStatusWhileImport = saveStatusWhileImport;
	}
	

	

	/*
	 * Import action / marker column read from the LAST column of the import Excel.
	 * Business values: A/ADD/Add (create), U/update/Update (update), D/delete/Delete
	 * (soft delete). Blank or any non-delete value falls through to the existing
	 * create-or-update path, which is left exactly as it was.
	 */
	private String importAction=null;

	public String getImportAction() {
		return importAction;
	}

	public void setImportAction(String importAction) {
		this.importAction = importAction;
	}
}
