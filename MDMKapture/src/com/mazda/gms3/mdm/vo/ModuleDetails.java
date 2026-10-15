package com.mazda.gms3.mdm.vo;

import java.util.ArrayList;

public class ModuleDetails{
	
	private Long roleId=null;
	private Integer accessType=null;
	
	
	private Long moduleId=null;
	private String moduleRefkey=null;
	private String moduleDescription=null;
	private String moduleDisplayName=null;
	// Default / SST / 
	private Integer moduleType=null;
	private String modulePath=null;
	
	private ArrayList<ModuleDetails> itemDetails = new ArrayList<ModuleDetails>();

	private String itemName=null;
	private Integer itemCode=null;
	private boolean itemSelected=false;
	
	// USED FOR DEFINING THE DISPLAY ORDER OF TILES ON DASHBOARD
	private Integer displayOrder=null;
	
	
	
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getModuleDisplayName() {
		return moduleDisplayName;
	}

	public void setModuleDisplayName(String moduleDisplayName) {
		this.moduleDisplayName = moduleDisplayName;
	}

	public Integer getAccessType() {
		return accessType;
	}

	public void setAccessType(Integer accessType) {
		this.accessType = accessType;
	}

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public boolean isItemSelected() {
		return itemSelected;
	}

	public void setItemSelected(boolean itemSelected) {
		this.itemSelected = itemSelected;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public Integer getItemCode() {
		return itemCode;
	}

	public void setItemCode(Integer itemCode) {
		this.itemCode = itemCode;
	}

	public ArrayList<ModuleDetails> getItemDetails() {
		return itemDetails;
	}

	public void setItemDetails(ArrayList<ModuleDetails> itemDetails) {
		this.itemDetails = itemDetails;
	}

	public Long getModuleId() {
		return moduleId;
	}

	public void setModuleId(Long moduleId) {
		this.moduleId = moduleId;
	}

	public String getModuleRefkey() {
		return moduleRefkey;
	}

	public void setModuleRefkey(String moduleRefkey) {
		this.moduleRefkey = moduleRefkey;
	}

	public String getModuleDescription() {
		return moduleDescription;
	}

	public void setModuleDescription(String moduleDescription) {
		this.moduleDescription = moduleDescription;
	}

	public Integer getModuleType() {
		return moduleType;
	}

	public void setModuleType(Integer moduleType) {
		this.moduleType = moduleType;
	}

	public String getModulePath() {
		return modulePath;
	}

	public void setModulePath(String modulePath) {
		this.modulePath = modulePath;
	}

}