package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.RoleDetails;

public class AccessManagementBean {
	
	private String errorMessage=null;
	private String successMessage=null;
	private ArrayList<RoleDetails> rolesList = null;
	private String roleId=null;
	
	private ArrayList<ModuleDetails> modulesList = null;
	private String selectedModules=null;
	
	private ArrayList<ModuleDetails> itemsList = null;
	
	private boolean showReadControls=false;
	private boolean showWriteControls = false;
	
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
	
	public ArrayList<ModuleDetails> getItemsList() {
		return itemsList;
	}
	public void setItemsList(ArrayList<ModuleDetails> itemsList) {
		this.itemsList = itemsList;
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
	public ArrayList<RoleDetails> getRolesList() {
		return rolesList;
	}
	public void setRolesList(ArrayList<RoleDetails> rolesList) {
		this.rolesList = rolesList;
	}
	public String getRoleId() {
		return roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	public ArrayList<ModuleDetails> getModulesList() {
		return modulesList;
	}
	public void setModulesList(ArrayList<ModuleDetails> modulesList) {
		this.modulesList = modulesList;
	}
	public String getSelectedModules() {
		return selectedModules;
	}
	public void setSelectedModules(String selectedModules) {
		this.selectedModules = selectedModules;
	}
}