package com.mazda.gms3.dmt.vo;

import java.util.ArrayList;

/**
 * THE LOGGED-IN USER'S PROFILE, AS READ FROM KAPTURE.
 *
 * Replaces the InfoManager <code>UserITO</code> that
 * <code>FetchUserProfileImpl.getUserProfileDetailsFromIM()</code> returned. It carries only
 * what DMT actually read from that object:
 *
 * <pre>
 *   UserITO.getFirstName()                        -> firstName
 *   UserITO.getEmail()                            -> email
 *   UserITO.getDefaultLocale().getRecordID()      -> defaultLocale
 *   UserITO.getSecurityRoles()   .getReferenceKey -> roleRefKeys
 * </pre>
 *
 * <code>roleRefKeys</code> holds the reference keys exactly as Kapture spells them. Nothing is
 * filtered here: the caller compares them against DMT_ALLOWED_ACCESS_ROLES.
 */
public class UserProfileDetails {

	private String firstName = null;

	private String email = null;

	private String defaultLocale = null;

	private ArrayList<String> roleRefKeys = new ArrayList<String>();

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDefaultLocale() {
		return defaultLocale;
	}

	public void setDefaultLocale(String defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	public ArrayList<String> getRoleRefKeys() {
		return roleRefKeys;
	}

	public void setRoleRefKeys(ArrayList<String> roleRefKeys) {
		this.roleRefKeys = roleRefKeys;
	}
}
