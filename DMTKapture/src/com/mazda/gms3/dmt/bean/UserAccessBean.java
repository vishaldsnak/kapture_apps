package com.mazda.gms3.dmt.bean;

import java.util.ArrayList;

public class UserAccessBean {

	private String loggedInUserId = null;
	// SET SHOW NO ACCESS BY DEFAULT TO TRUE
	private boolean showNoAccess = true;
	private ArrayList<String> mappedRolesList = null;
	private String userDisplayName = null;

	public String getLoggedInUserId() {
		return loggedInUserId;
	}

	public void setLoggedInUserId(String loggedInUserId) {
		this.loggedInUserId = loggedInUserId;
	}

	public boolean isShowNoAccess() {
		return showNoAccess;
	}

	public void setShowNoAccess(boolean showNoAccess) {
		this.showNoAccess = showNoAccess;
	}

	public ArrayList<String> getMappedRolesList() {
		return mappedRolesList;
	}

	public void setMappedRolesList(ArrayList<String> mappedRolesList) {
		this.mappedRolesList = mappedRolesList;
	}

	public String getUserDisplayName() {
		return userDisplayName;
	}

	public void setUserDisplayName(String userDisplayName) {
		this.userDisplayName = userDisplayName;
	}

}