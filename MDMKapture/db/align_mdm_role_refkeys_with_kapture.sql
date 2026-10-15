-- ===========================================================================
--  ALIGN THE MDM ROLE REFERENCE KEYS WITH KAPTURE
--
--  Run against: kapture_dc   (the MDM schema - NOT kapture_cms_db)
--  Run once per environment (local / VDI / MC Dev). Safe to re-run.
--
--  WHY
--  ---------------------------------------------------------------------------
--  Until now a user's mapped roles came from InfoManager, via
--  FetchUserProfileImpl.getUserProfileDetailsFromIM(). InfoManager is gone, so
--  AccessManagementFilter now reads them from Kapture instead:
--
--      kapture_cms_db.k_add_user_cmsrole.role_ref   WHERE userid = <iv-user>
--
--  Kapture spells the same five roles with a trailing _KEY:
--
--      MDM master (before)          Kapture k_add_user_cmsrole.role_ref
--      -------------------------    ---------------------------------
--      MDM_MAZDA_ACE_ADMIN          MDM_MAZDA_ACE_ADMIN_KEY
--      MDM_DISTRIBUTOR_ADMIN        MDM_DISTRIBUTOR_ADMIN_KEY
--      MDM_DISTRIBUTOR_AUTHOR       MDM_DISTRIBUTOR_AUTHOR_KEY
--      MDM_MC_CDROM_USER            MDM_MC_CDROM_USER_KEY
--      DEFAULT_ADMINISTRATION_ROLE  (does not exist in Kapture yet - see below)
--
--  Rather than carry a translation matrix in code, the MDM master is renamed to
--  match Kapture exactly, so the lookup is plain equality and a role added in
--  Kapture later needs no code change. That table's role reference key is only
--  ever compared, never joined on: gms3_mdm_roles_access keys off mdm_role_id
--  (the only foreign key on gms3_mdm_roles), and mdm_role_refkey appears in no
--  other table in this schema. So this rename cannot orphan an access row.
--
--  KAUTHOR ROLES ARE NOT OUR CONCERN. k_add_user_cmsrole also holds CEC_*,
--  MC_*, MNAO_*, CMS_* and ADMIN__ROLE_KEY. They are simply not present in this
--  master, so they match nothing and are ignored. No MDM role master row is
--  created for them.
--
--  *** OPEN ITEM - MDM SUPER ADMIN ***
--  Kapture currently defines only FOUR MDM roles. There is no
--  DEFAULT_ADMINISTRATION_ROLE_KEY, and no user holds one. Super admin is
--  derived from mdm_role_priority = 1 (AccessManagementInterface
--  .SUPER_ADMIN_ROLE_PRIORITY), so until that role is created in Kapture AND
--  assigned to someone, NO USER RESOLVES TO SUPER ADMIN and nobody sees the
--  Administration tab. The row is renamed here anyway so that the moment
--  Kapture creates it, it works with no further change.
-- ===========================================================================

USE kapture_dc;

-- Before -------------------------------------------------------------------
SELECT 'BEFORE' AS stage, mdm_role_id, mdm_role_priority, mdm_role_name, mdm_role_refkey
FROM   gms3_mdm_roles
ORDER  BY mdm_role_priority;

-- Rename -------------------------------------------------------------------
-- Guarded with NOT LIKE '%\_KEY' so a second run is a no-op rather than
-- producing MDM_MAZDA_ACE_ADMIN_KEY_KEY.
UPDATE gms3_mdm_roles
SET    mdm_role_refkey = CONCAT(mdm_role_refkey, '_KEY')
WHERE  mdm_role_refkey IN ('DEFAULT_ADMINISTRATION_ROLE',
                           'MDM_MAZDA_ACE_ADMIN',
                           'MDM_DISTRIBUTOR_ADMIN',
                           'MDM_DISTRIBUTOR_AUTHOR',
                           'MDM_MC_CDROM_USER')
AND    mdm_role_refkey NOT LIKE '%\_KEY';

-- After --------------------------------------------------------------------
SELECT 'AFTER' AS stage, mdm_role_id, mdm_role_priority, mdm_role_name, mdm_role_refkey
FROM   gms3_mdm_roles
ORDER  BY mdm_role_priority;

-- ===========================================================================
--  ALSO CHANGE, IN THE SAME DEPLOYMENT (they are not database values):
--
--    WEB-INF/classes/application.properties
--      wsl.default.user.roles                   -> DEFAULT_ADMINISTRATION_ROLE_KEY
--      rmitool.MDM_DISTRIBUTOR_AUTHOR.refkey    -> MDM_DISTRIBUTOR_AUTHOR_KEY
--      dataexttool.MDM_DISTRIBUTOR_AUTHOR.refkey-> MDM_DISTRIBUTOR_AUTHOR_KEY
--
--    AccessManagementFilter.java
--      the blank/missing fallback literal       -> DEFAULT_ADMINISTRATION_ROLE_KEY
--
--  All four are already updated in the source tree; this note is for anyone
--  applying the SQL to an environment whose WAR predates that change.
-- ===========================================================================
