package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.UserProfileDetails;

/**
 * THE LOGGED-IN USER'S PROFILE, FROM KAPTURE INSTEAD OF INFOMANAGER.
 *
 * Replaces com.mazda.gms3.mdm.im.impl.FetchUserProfileImpl, which called
 * <code>IQServiceClient.getUserRequest().getUserByLogin(wslId)</code> over SOAP and returned a
 * <code>UserITO</code>. InfoManager is not deployed alongside Kapture, so on the migrated
 * platform that call cannot succeed and every login would fall into the filter's error branch.
 *
 * Three reads on the Kapture CMS schema, keyed on the <code>iv-user</code> id:
 *
 * <pre>
 *   k_user                 firstname, cms_defaultlocale     (one row, the user)
 *   k_user_content_locale  locale                           (many rows)
 *   k_add_user_cmsrole     role_ref                         (many rows)
 * </pre>
 *
 * WHY THE LOCALES COME FROM THE CHILD TABLE. k_user has a cms_contentlocale column that looks
 * like it should hold them, and it is NULL for every user checked - the values live in
 * k_user_content_locale. Reading the column returns an empty locale list and the user then sees
 * only their default locale, with no error anywhere.
 *
 * WHY THE ROLE KEYS ARE NOT FILTERED HERE. k_add_user_cmsrole mixes MDM roles with KAuthor's own
 * (CEC_*, MC_*, MNAO_*, CMS_*, ADMIN__ROLE_KEY). They are returned as-is and matched against the
 * MDM roles master by the caller, where the master already is. MDM deliberately holds no role
 * master row for a KAuthor role, so those keys match nothing and drop out. The MDM master was
 * renamed to Kapture's spelling (the trailing _KEY) precisely so that no mapping table is needed
 * - see db/align_mdm_role_refkeys_with_kapture.sql.
 *
 * A NULL RETURN MEANS "NO SUCH ACTIVE USER" and the caller must treat it exactly as it treated a
 * failed InfoManager lookup: reset the session bean and redirect to error. It must NOT be
 * confused with a user who exists but has no MDM role - that one returns a profile with an empty
 * role list and correctly ends up seeing no modules.
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
			String sql = "SELECT firstname, cms_defaultlocale FROM kapture_cms_db.k_user"
					+ " WHERE TRIM(LOWER(userid)) = ? AND "
					+ Utilities.activeUserStatusPredicate("user_status");
			pstmt = cmsConn.prepareStatement(sql);
			pstmt.setString(1, userId.toLowerCase());
			rs = pstmt.executeQuery();
			if (rs.next()) {
				profile = new UserProfileDetails();
				profile.setFirstName(rs.getString("firstname"));
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
			 * 2. CONTENT LOCALES. Duplicates are dropped here so the caller's list stays clean;
			 * it de-duplicates again against the default locale, which it adds first.
			 */
			sql = "SELECT DISTINCT locale FROM kapture_cms_db.k_user_content_locale"
					+ " WHERE TRIM(LOWER(userid)) = ? AND locale IS NOT NULL AND TRIM(locale) <> ''";
			pstmt = cmsConn.prepareStatement(sql);
			pstmt.setString(1, userId.toLowerCase());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String locale = rs.getString("locale");
				if (null != locale && !"".equals(locale.trim())
						&& !profile.getContentLocales().contains(locale.trim())) {
					profile.getContentLocales().add(locale.trim());
				}
				locale = null;
			}
			rs.close();
			rs = null;
			pstmt.close();
			pstmt = null;
			sql = null;

			/*
			 * 3. ROLE REFERENCE KEYS - every role Kapture has mapped to this user, MDM's and
			 * KAuthor's alike. The caller decides which of them mean anything to MDM.
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
			 * EXTRA ROLES FOR LOCAL TESTING ONLY - kapture.user.additional.roles.
			 *
			 * Kapture defines no DEFAULT_ADMINISTRATION_ROLE_KEY yet, so no user can resolve to
			 * MDM Super Admin and the Administration screens cannot be reached at all. This
			 * property appends reference keys to whatever Kapture returned, so a workstation can
			 * exercise the super-admin path against the REAL profile lookup rather than the
			 * hardcoded default-user block.
			 *
			 * It grants nothing by itself: the caller still intersects these with the MDM roles
			 * master (gms3_mdm_roles) and still derives super-admin from mdm_role_priority = 1,
			 * so a key that is not a real MDM role is ignored exactly like a KAuthor one.
			 *
			 * MUST BE BLANK ON VDI AND MC DEV. Logged loudly every time it adds anything, so a
			 * environment that was left configured is obvious in the log rather than silently
			 * handing out administration rights.
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
				if (!added.isEmpty()) {
					logger.info("getUserProfileFromCMS :: *** kapture.user.additional.roles IS SET"
							+ " *** added " + added + " to the roles Kapture returned for {"
							+ userId + "}. THIS IS A LOCAL TESTING SWITCH - it must be blank on"
							+ " VDI and MC Dev.");
				}
				added = null;
			}
			extraRoles = null;

			/*
			 * NO FALLBACK FOR A MISSING DEFAULT LOCALE, DELIBERATELY. cms_defaultlocale is NULL
			 * for some Kapture users (7 of 46 on the dev CMS). That is a DATA problem in Kapture,
			 * and the correct behaviour is the one the InfoManager path had: the caller sees an
			 * unusable profile and sends the user to no-access. Substituting a configured locale
			 * would let an incomplete profile through and hide the gap from whoever has to fix
			 * it. Logged here so the reason is in the log rather than inferred from a bare
			 * redirect.
			 */
			if (null == profile.getDefaultLocale() || "".equals(profile.getDefaultLocale().trim())) {
				logger.info("getUserProfileFromCMS :: {" + userId + "} has no cms_defaultlocale in"
						+ " Kapture. Profile is unusable - the user will be sent to no-access."
						+ " Fix the user's default locale in Kapture.");
			}

			logger.info("getUserProfileFromCMS :: {" + userId + "} defaultLocale {"
					+ profile.getDefaultLocale() + "} contentLocales "
					+ profile.getContentLocales().size() + " roleRefKeys "
					+ profile.getRoleRefKeys());
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(UserProfileDAO.class.getName(),
					"getUserProfileFromCMS()", e);
			/*
			 * A DATABASE FAILURE IS NOT AN UNKNOWN USER, but the caller has only the two outcomes
			 * the InfoManager path had, and letting a half-populated profile through would grant
			 * whatever access the partial role list happened to resolve to. Fail closed.
			 */
			profile = null;
		} finally {
			try {
				if (null != rs) {
					rs.close();
				}
			} catch (Exception e) {
				// IGNORE - NOTHING USEFUL CAN BE DONE WHILE CLOSING
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
	 * The role reference keys this user holds that MDM actually knows about.
	 *
	 * @param profile      the Kapture profile, may be null
	 * @param mdmRoleRefKeys the reference keys from the MDM roles master (gms3_mdm_roles)
	 * @return the intersection, compared case insensitively; never null
	 */
	public static ArrayList<String> mdmRolesOnly(UserProfileDetails profile,
			ArrayList<String> mdmRoleRefKeys) {
		ArrayList<String> matched = new ArrayList<String>();
		if (null == profile || null == mdmRoleRefKeys) {
			return matched;
		}
		for (int i = 0; i < profile.getRoleRefKeys().size(); i++) {
			String held = profile.getRoleRefKeys().get(i);
			if (null == held || "".equals(held.trim())) {
				continue;
			}
			for (int j = 0; j < mdmRoleRefKeys.size(); j++) {
				String known = mdmRoleRefKeys.get(j);
				if (null != known && held.trim().equalsIgnoreCase(known.trim())
						&& !matched.contains(known.trim())) {
					matched.add(known.trim());
					break;
				}
			}
			held = null;
		}
		return matched;
	}

}
