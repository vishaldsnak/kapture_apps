package com.mazda.gms3.mdm.vo;

import java.sql.Timestamp;
import java.util.ArrayList;

public class VinDetails {
	
	private int srNo;
	private Long vinId=null;
	private Long countryLocaleId=null;
	private String countryLocaleCode=null;
	private Long manualLanguageId=null;
	private String manualLanguageCode=null;
	private String carlineCode=null;
	private String groupCode=null;
	private String wmiCode=null;
	private String vdsCode=null;
	private String visStartRange=null;
	private String visEndRange=null;
	private String engineCode=null;
	private String missionCode=null;
	private String flag=null;
	private String flagLabel=null;
	private Timestamp entryTime=null;
	private Timestamp updatedTime=null;
	private boolean editableFlag = false;
	private boolean showCheckBox = true;
	
	
	private String carlineNameEng=null;
	private String carlineNameReg=null;
	private String icDisplayCarlineCode=null;
	
	private ArrayList<SelectItemDetails> wmiCodeList =  null;
	
	private String oldWmiCode=null;
	private String oldVdsCode=null;
	private String oldVisStartRange=null;
	private String oldVisEndRange=null;
	private String oldFlag=null;
	private String syncStatus=null;
	
	
	public String getIcDisplayCarlineCode() {
		return icDisplayCarlineCode;
	}
	public void setIcDisplayCarlineCode(String icDisplayCarlineCode) {
		this.icDisplayCarlineCode = icDisplayCarlineCode;
	}
	public String getOldWmiCode() {
		return oldWmiCode;
	}
	public void setOldWmiCode(String oldWmiCode) {
		this.oldWmiCode = oldWmiCode;
	}
	public String getOldVdsCode() {
		return oldVdsCode;
	}
	public void setOldVdsCode(String oldVdsCode) {
		this.oldVdsCode = oldVdsCode;
	}
	public String getOldVisStartRange() {
		return oldVisStartRange;
	}
	public void setOldVisStartRange(String oldVisStartRange) {
		this.oldVisStartRange = oldVisStartRange;
	}
	public String getOldVisEndRange() {
		return oldVisEndRange;
	}
	public void setOldVisEndRange(String oldVisEndRange) {
		this.oldVisEndRange = oldVisEndRange;
	}
	public String getOldFlag() {
		return oldFlag;
	}
	public void setOldFlag(String oldFlag) {
		this.oldFlag = oldFlag;
	}
	public String getSyncStatus() {
		return syncStatus;
	}
	public void setSyncStatus(String syncStatus) {
		this.syncStatus = syncStatus;
	}
	public ArrayList<SelectItemDetails> getWmiCodeList() {
		return wmiCodeList;
	}
	public void setWmiCodeList(ArrayList<SelectItemDetails> wmiCodeList) {
		this.wmiCodeList = wmiCodeList;
	}
	public String getCountryLocaleCode() {
		return countryLocaleCode;
	}
	public void setCountryLocaleCode(String countryLocaleCode) {
		this.countryLocaleCode = countryLocaleCode;
	}
	public String getCarlineNameEng() {
		return carlineNameEng;
	}
	public void setCarlineNameEng(String carlineNameEng) {
		this.carlineNameEng = carlineNameEng;
	}
	public String getCarlineNameReg() {
		return carlineNameReg;
	}
	public void setCarlineNameReg(String carlineNameReg) {
		this.carlineNameReg = carlineNameReg;
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
	public Long getVinId() {
		return vinId;
	}
	public void setVinId(Long vinId) {
		this.vinId = vinId;
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
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getGroupCode() {
		return groupCode;
	}
	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
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
	public String getEngineCode() {
		return engineCode;
	}
	public void setEngineCode(String engineCode) {
		this.engineCode = engineCode;
	}
	public String getMissionCode() {
		return missionCode;
	}
	public void setMissionCode(String missionCode) {
		this.missionCode = missionCode;
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
	public Long getCountryLocaleId() {
		return countryLocaleId;
	}
	public void setCountryLocaleId(Long countryLocaleId) {
		this.countryLocaleId = countryLocaleId;
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