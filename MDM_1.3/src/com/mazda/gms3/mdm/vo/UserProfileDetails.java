package com.mazda.gms3.mdm.vo;

import java.util.ArrayList;

/**
 * THE LOGGED-IN USER'S PROFILE, AS READ FROM KAPTURE.
 *
 * This is the replacement for the InfoManager <code>UserITO</code> that
 * <code>FetchUserProfileImpl.getUserProfileDetailsFromIM()</code> used to return. It carries
 * ONLY the four things AccessManagementFilter actually consumed from that object - the ITO
 * exposed a great deal more, none of which was ever read:
 *
 * <pre>
 *   UserITO.getFirstName()                        -> firstName
 *   UserITO.getDefaultLocale().getRecordID()      -> defaultLocale
 *   UserITO.getContentLocales()  .getRecordID()   -> contentLocales
 *   UserITO.getSecurityRoles()   .getReferenceKey -> roleRefKeys
 * </pre>
 *
 * The locales are kept in the UNDERSCORE form Kapture stores ("en_US"), which is also the form
 * InfoManager's getRecordID() returned, so the filter's existing replace("_","-") continues to
 * do the conversion in one place.
 *
 * <code>roleRefKeys</code> holds the reference keys EXACTLY as Kapture spells them, KAuthor's
 * included. Nothing is filtered here on purpose: the caller compares them against the MDM roles
 * master (gms3_mdm_roles), and anything that is not an MDM role simply fails to match. See
 * db/align_mdm_role_refkeys_with_kapture.sql for why no translation table is needed.
 */
public class UserProfileDetails {

	private String firstName = null;

	private String defaultLocale = null;

	private ArrayList<String> contentLocales = new ArrayList<String>();

	private ArrayList<String> roleRefKeys = new ArrayList<String>();

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getDefaultLocale() {
		return defaultLocale;
	}

	public void setDefaultLocale(String defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	public ArrayList<String> getContentLocales() {
		return contentLocales;
	}

	public void setContentLocales(ArrayList<String> contentLocales) {
		this.contentLocales = contentLocales;
	}

	public ArrayList<String> getRoleRefKeys() {
		return roleRefKeys;
	}

	public void setRoleRefKeys(ArrayList<String> roleRefKeys) {
		this.roleRefKeys = roleRefKeys;
	}

}
