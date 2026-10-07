package com.mazda.gms3.dmt.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.UserProfileDetails;

/**
 * THE LOGGED-IN USER'S PROFILE, FROM KAPTURE INSTEAD OF INFOMANAGER.
 *
 * Replaces com.mazda.gms3.dmt.conversion.impl.FetchUserProfileImpl, which called
 * <code>IQServiceClient.getUserRequest().getUserByLogin(wslId)</code> over SOAP. Same reads as
 * the MDM application makes, keyed on the <code>iv-user</code> id:
 *
 * <pre>
 *   k_user              firstname, email, cms_defaultlocale   (one row, the user)
 *   k_add_user_cmsrole  role_ref                              (many rows)
 * </pre>
 *
 * A NULL RETURN MEANS "NO SUCH ACTIVE USER" (or the CMS database could not be read) and the
 * caller must treat it exactly as it treated a failed InfoManager lookup. A user who exists but
 * holds no DMT role returns a profile with a role list that matches nothing, and ends up on the
 * no-access page.
 */
public class UserProfileDAO extends DBConnectionHelper {

	private static Logger logger = LogManager.getLogger(UserProfileDAO.class);

	public static UserProfileDetails getUserProfileFromCMS(String wslId) {
		UserProfileDetails profile = null;
		Connection cmsConn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			if (null == wslId || "".equals(wslId.trim())) {
				logger.info("getUserProfileFromCMS :: wsl id as parameter is null.");
				return null;
			}
			String userId = wslId.trim();

			cmsConn = DBConnectionHelper.getCMSConnection();
			if (null == cmsConn) {
				logger.info("getUserProfileFromCMS :: no Kapture CMS connection - cannot resolve a"
						+ " profile for {" + userId + "}.");
				return null;
			}

			/*
			 * 1. THE USER. user_status is compared through the shared predicate because Kapture
			 * writes it inconsistently - "Active" for one user and "ACTIVE" for the next.
			 */
			String sql = "SELECT firstname, email, cms_defaultlocale FROM kapture_cms_db.k_user"
					+ " WHERE TRIM(LOWER(userid)) = ? AND "
					+ Utilities.activeUserStatusPredicate("user_status");
			pstmt = cmsConn.prepareStatement(sql);
			pstmt.setString(1, userId.toLowerCase());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				profile = new UserProfileDetails();
				profile.setFirstName(rs.getString("firstname"));
				profile.setEmail(rs.getString("email"));
				profile.setDefaultLocale(rs.getString("cms_defaultlocale"));
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			sql = null;

			if (null == profile) {
				logger.info("getUserProfileFromCMS :: no ACTIVE k_user row for {" + userId
						+ "}. Treating as an unknown user.");
				return null;
			}

			/*
			 * 2. ROLE REFERENCE KEYS - every role Kapture has mapped to this user. The caller
			 * decides which of them give access to DMT (DMT_ALLOWED_ACCESS_ROLES).
			 */
			sql = "SELECT DISTINCT role_ref FROM kapture_cms_db.k_add_user_cmsrole"
					+ " WHERE TRIM(LOWER(userid)) = ? AND role_ref IS NOT NULL AND TRIM(role_ref) <> ''";
			pstmt = cmsConn.prepareStatement(sql);
			pstmt.setString(1, userId.toLowerCase());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String roleRef = rs.getString("role_ref");
				if (null != roleRef && !"".equals(roleRef.trim())
						&& !profile.getRoleRefKeys().contains(roleRef.trim())) {
					profile.getRoleRefKeys().add(roleRef.trim());
				}
				roleRef = null;
			}

			/*
			 * EXTRA ROLES - kapture.user.additional.roles. Appends reference keys to whatever
			 * Kapture returned, for an environment where the administration role is not yet
			 * defined in Kapture. It grants nothing by itself: the caller still intersects the
			 * list with DMT_ALLOWED_ACCESS_ROLES. MUST BE BLANK ON MC DEV. Logged every time it
			 * adds anything.
			 */
			String extraRoles = ApplicationProperties.getProperty("kapture.user.additional.roles");
			if (null != extraRoles && !"".equals(extraRoles.trim())) {
				String[] keys = extraRoles.trim().split(",");
				ArrayList<String> added = new ArrayList<String>();
				for (int i = 0; i < keys.length; i++) {
					String key = (null == keys[i]) ? "" : keys[i].trim();
					if (!"".equals(key) && !profile.getRoleRefKeys().contains(key)) {
						profile.getRoleRefKeys().add(key);
						added.add(key);
					}
					key = null;
				}
				keys = null;
				// not logged: this runs on every page request and filled the log
				// if (!added.isEmpty()) {
				// 	logger.info("getUserProfileFromCMS :: *** kapture.user.additional.roles IS SET"
				// 			+ " *** added " + added + " to the roles Kapture returned for {"
				// 			+ userId + "}. It must be blank on MC Dev.");
				// }
				added = null;
			}
			extraRoles = null;

			// not logged: this runs on every page request and filled the log
			// logger.info("getUserProfileFromCMS :: {" + userId + "} defaultLocale {"
			// 		+ profile.getDefaultLocale() + "} roleRefKeys " + profile.getRoleRefKeys());
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(UserProfileDAO.class.getName(),
					"getUserProfileFromCMS()", e);
			// A HALF-POPULATED PROFILE MUST NOT BE LET THROUGH. FAIL CLOSED.
			profile = null;
		} finally {
			try {
				if (null != rs) {
					rs.close();
				}
			} catch (Exception e) {
				// IGNORE
			}
			try {
				if (null != pstmt) {
					pstmt.close();
				}
			} catch (Exception e) {
				// IGNORE
			}
			try {
				if (null != cmsConn) {
					cmsConn.close();
				}
			} catch (Exception e) {
				// IGNORE
			}
			rs = null;
			pstmt = null;
			cmsConn = null;
		}
		return profile;
	}

	/**
	 * E-mail address of an active Kapture user, for the schedule notification e-mail.
	 *
	 * @return the address, or null when the user is unknown, inactive or has none
	 */
	public static String getUserEmail(String wslId) {
		UserProfileDetails profile = getUserProfileFromCMS(wslId);
		if (null != profile && null != profile.getEmail() && !"".equals(profile.getEmail().trim())) {
			return profile.getEmail().trim();
		}
		return null;
	}
}
