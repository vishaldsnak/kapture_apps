package com.mazda.gms3.mdm.bean;

import java.util.ArrayList;

import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.ModuleDetails;

public class UserAccessBean {

	private String dashboardRefKey = AccessManagementInterface.REF_KEY_DASHBOARD;
	private String loggedInUserId = null;
	private String defaultLocale = null;
	private ArrayList<String> userLocalesList = null;
	private ArrayList<String> mappedRolesList = null;
	private boolean superAdminUser = false;

	private ArrayList<ModuleDetails> defaultModulesList = null;
	private ArrayList<ModuleDetails> normalModulesList = null;
	private ArrayList<ModuleDetails> engineModulesList = null;
	private ArrayList<ModuleDetails> missionModulesList = null;
	private ArrayList<ModuleDetails> sstMaintenanceModulesList = null;
	private ArrayList<ModuleDetails> sstVehicleTypeModulesList = null;
	private ArrayList<ModuleDetails> vehilceTypeModulesList = null;
	
	private ArrayList<ModuleDetails> rumVinMappingList=null;

	private ArrayList<ModuleDetails> userAllModulesList = null;
	private ArrayList<ModuleDetails> topMenuList = null;
	private String userDisplayName = null;
	private ArrayList<ModuleDetails> cdCreationModulesList = null;

	
	private ArrayList<ModuleDetails> displayTilesList = null;
	
	public ArrayList<ModuleDetails> getDisplayTilesList() {
		return displayTilesList;
	}

	public void setDisplayTilesList(ArrayList<ModuleDetails> displayTilesList) {
		this.displayTilesList = displayTilesList;
	}

	public ArrayList<ModuleDetails> getRumVinMappingList() {
		return rumVinMappingList;
	}

	public void setRumVinMappingList(ArrayList<ModuleDetails> rumVinMappingList) {
		this.rumVinMappingList = rumVinMappingList;
	}

	public ArrayList<ModuleDetails> getCdCreationModulesList() {
		return cdCreationModulesList;
	}

	public void setCdCreationModulesList(
			ArrayList<ModuleDetails> cdCreationModulesList) {
		this.cdCreationModulesList = cdCreationModulesList;
	}

	public String getUserDisplayName() {
		return userDisplayName;
	}

	public void setUserDisplayName(String userDisplayName) {
		this.userDisplayName = userDisplayName;
	}

	public ArrayList<ModuleDetails> getTopMenuList() {
		return topMenuList;
	}

	public void setTopMenuList(ArrayList<ModuleDetails> topMenuList) {
		this.topMenuList = topMenuList;
	}

	public ArrayList<ModuleDetails> getUserAllModulesList() {
		return userAllModulesList;
	}

	public void setUserAllModulesList(
			ArrayList<ModuleDetails> userAllModulesList) {
		this.userAllModulesList = userAllModulesList;
	}

	public ArrayList<String> getMappedRolesList() {
		return mappedRolesList;
	}

	public void setMappedRolesList(ArrayList<String> mappedRolesList) {
		this.mappedRolesList = mappedRolesList;
	}

	public boolean isSuperAdminUser() {
		return superAdminUser;
	}

	public void setSuperAdminUser(boolean superAdminUser) {
		this.superAdminUser = superAdminUser;
	}

	public String getLoggedInUserId() {
		return loggedInUserId;
	}

	public void setLoggedInUserId(String loggedInUserId) {
		this.loggedInUserId = loggedInUserId;
	}

	public String getDefaultLocale() {
		return defaultLocale;
	}

	public void setDefaultLocale(String defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	public ArrayList<String> getUserLocalesList() {
		return userLocalesList;
	}

	public void setUserLocalesList(ArrayList<String> userLocalesList) {
		this.userLocalesList = userLocalesList;
	}

	public ArrayList<ModuleDetails> getDefaultModulesList() {
		return defaultModulesList;
	}

	public void setDefaultModulesList(
			ArrayList<ModuleDetails> defaultModulesList) {
		this.defaultModulesList = defaultModulesList;
	}

	public ArrayList<ModuleDetails> getNormalModulesList() {
		return normalModulesList;
	}

	public void setNormalModulesList(ArrayList<ModuleDetails> normalModulesList) {
		this.normalModulesList = normalModulesList;
	}

	public ArrayList<ModuleDetails> getEngineModulesList() {
		return engineModulesList;
	}

	public void setEngineModulesList(ArrayList<ModuleDetails> engineModulesList) {
		this.engineModulesList = engineModulesList;
	}

	public ArrayList<ModuleDetails> getMissionModulesList() {
		return missionModulesList;
	}

	public void setMissionModulesList(
			ArrayList<ModuleDetails> missionModulesList) {
		this.missionModulesList = missionModulesList;
	}

	public ArrayList<ModuleDetails> getSstMaintenanceModulesList() {
		return sstMaintenanceModulesList;
	}

	public void setSstMaintenanceModulesList(
			ArrayList<ModuleDetails> sstMaintenanceModulesList) {
		this.sstMaintenanceModulesList = sstMaintenanceModulesList;
	}

	public ArrayList<ModuleDetails> getSstVehicleTypeModulesList() {
		return sstVehicleTypeModulesList;
	}

	public void setSstVehicleTypeModulesList(
			ArrayList<ModuleDetails> sstVehicleTypeModulesList) {
		this.sstVehicleTypeModulesList = sstVehicleTypeModulesList;
	}

	public ArrayList<ModuleDetails> getVehilceTypeModulesList() {
		return vehilceTypeModulesList;
	}

	public void setVehilceTypeModulesList(
			ArrayList<ModuleDetails> vehilceTypeModulesList) {
		this.vehilceTypeModulesList = vehilceTypeModulesList;
	}

	public String getDashboardRefKey() {
		return dashboardRefKey;
	}

	public void setDashboardRefKey(String dashboardRefKey) {
		this.dashboardRefKey = dashboardRefKey;
	}
}