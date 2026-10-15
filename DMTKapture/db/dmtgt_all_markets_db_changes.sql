-- ---------------------------------------------------------------------------------------------
-- DMT (dmtgt) - ALL DATABASE CHANGES FOR ALL MARKETS (MC, MME, MNAO) IN ONE SCRIPT
-- ---------------------------------------------------------------------------------------------
-- Replaces the per-release scripts dmtgt_pre_deployment.sql, dmtgt_preview_docs.sql, dmtgt_publish.sql,
-- dmtgt_mme_pre_deployment.sql, dmtgt_mnao_pre_deployment.sql, dmtgt_ddl_reference.sql, dmtgt_mnao_ddl.sql.
-- Run it ONCE per environment (QA: as part of the one-time migration), then deploy dmtgt.war - nothing
-- else is needed on the database for DMT.
--
-- WHEN TO RUN - AFTER the table / data migration of kapture_dc is COMPLETE:
--   1. Job 1 OracleToMySQLDataMigrationJob FULL has loaded the data
--   2. Job 2 OracleToMySQLConstraintsAndIndexesJob has created the keys and indexes
--      (the new tables in part A reference gms3_dmt_conv_schedule's primary key)
-- Job 1 FULL TRUNCATEs and reloads every table: if it runs again on these tables afterwards, the data
-- part (B) is undone - run part B again (it is safe to run again, see below).
--
-- Schema: kapture_dc (change the prefix if the target environment uses another schema name). MySQL 8.0.
--
-- HOW TO RUN (from 2026-10-09): with Job 2 OracleToMySQLConstraintsAndIndexesJob, OPTION 2 (DMT DB changes):
--         run.bat --option=2        (or run.bat and answer 2 at the prompt)
-- The job reads this file (property dmt.db.changes.file), runs part A as is, runs EVERY UPDATE in
-- primary-key chunks of dmt.db.changes.chunk.rows rows with a commit after each chunk, and writes the
-- part C results to its log and Excel report. Chunking is what makes part B safe on Group Replication
-- (MC QA: one UPDATE of ~293k rows was rolled back by the GR transaction size limit) and on any volume.
-- Plain SQL statements only (no procedures, no DELIMITER, no PREPARE). A manual run in a SQL client still
-- works on a SMALL database, but the UPDATEs are NOT chunked there - do not run it by hand on QA / PROD.
--
-- PARTS
--   A. Structure (ALTER TABLE / CREATE TABLE / CREATE INDEX)
--      A1 all markets  gms3_dmt_conv_schedule.dc_load_type; new table gms3_dmt_preview_docs
--      A2 MC           gms3_vc_japan_vin_details.vc_content_status
--      A3 MME          vc_content_status on the 14 gms3_vc_mme_vin_dtl_<locale> tables
--      A4 MNAO         dc_facelift_folder_name on gms3_dmt_<loc>_imdoc; new tables gms3_dmt_<loc>_dispord
--                      and gms3_dmt_<loc>_esicat; vc_content_status + display order columns on
--                      gms3_vc_vin_details / gms3_vc_model_year_details; index gms3_dmt_en_ca_imdoc_n2
--   B. Data
--      B1 all markets  vc_content_status = 'Published' on rows still without a status
--      B2 MNAO         dc_facelift_folder_name = 'FL0000' on existing document rows
--      B3 MNAO         ONE-TIME PATH REWRITE: 'FL0000\' after the 3rd backslash of every MNAO source path
--   C. Checks (read only)
--
-- vc_content_status: added with DEFAULT 'Published' and ALGORITHM=INSTANT, so every existing row reads
-- 'Published' without a table rewrite; the default is then set to NULL for new rows (DMT writes 'Draft',
-- the Publish Content job sets 'Published'). The values must match application.properties
-- view.content.status.draft / view.content.status.published. All ADD COLUMN use ALGORITHM=INSTANT.
--
-- RUNNING IT AGAIN is safe:
--   - ADD COLUMN / CREATE INDEX fail with "Duplicate column name" / "Duplicate key name" - harmless, it
--     means the change is there. In the mysql client use --force so it carries on past them:
--         mysql --force -h <host> -P <port> -u <user> -p kapture_dc < dmtgt_all_markets_db_changes.sql
--   - CREATE TABLE uses IF NOT EXISTS. The UPDATEs only touch rows that still need the change; the path
--     rewrite never changes a path twice (it skips a path whose 4th segment is already FL + 4 digits).
--
-- SIZE: every UPDATE below is ONE statement per table and column; Job 2 splits it into primary-key chunks
-- at run time, so nothing here depends on the ids of any one environment. (Until 2026-10-09 the path
-- rewrite carried fixed 25000-id ranges taken from MC Dev; on MC QA the ids are sparse and up to 260M,
-- so those ranges no longer fitted.) C0 (read only) shows the row count and highest id per table.
-- Part C must show 0 paths without the facelift folder.
-- ---------------------------------------------------------------------------------------------

-- ---------------------------------------------------------------------------------------------
-- C0. Before (read only): row count and highest id of the MNAO tables the path rewrite changes
-- ---------------------------------------------------------------------------------------------
SELECT 'gms3_dmt_en_us_imdoc' AS table_name, COUNT(*) AS row_count, MAX(dc_dd_id) AS max_id FROM kapture_dc.gms3_dmt_en_us_imdoc
UNION ALL
SELECT 'gms3_dmt_en_us_cat' AS table_name, COUNT(*) AS row_count, MAX(dc_cd_id) AS max_id FROM kapture_dc.gms3_dmt_en_us_cat
UNION ALL
SELECT 'gms3_dmt_en_us_vin' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_id) AS max_id FROM kapture_dc.gms3_dmt_en_us_vin
UNION ALL
SELECT 'gms3_dmt_en_us_vinattr' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_attr_id) AS max_id FROM kapture_dc.gms3_dmt_en_us_vinattr
UNION ALL
SELECT 'gms3_dmt_en_us_vinmaster' AS table_name, COUNT(*) AS row_count, MAX(dc_vm_id) AS max_id FROM kapture_dc.gms3_dmt_en_us_vinmaster
UNION ALL
SELECT 'gms3_dmt_en_us_cvc' AS table_name, COUNT(*) AS row_count, MAX(dc_cvc_id) AS max_id FROM kapture_dc.gms3_dmt_en_us_cvc
UNION ALL
SELECT 'gms3_dmt_en_us_inrlks' AS table_name, COUNT(*) AS row_count, MAX(dc_il_id) AS max_id FROM kapture_dc.gms3_dmt_en_us_inrlks
UNION ALL
SELECT 'gms3_dmt_es_mx_imdoc' AS table_name, COUNT(*) AS row_count, MAX(dc_dd_id) AS max_id FROM kapture_dc.gms3_dmt_es_mx_imdoc
UNION ALL
SELECT 'gms3_dmt_es_mx_cat' AS table_name, COUNT(*) AS row_count, MAX(dc_cd_id) AS max_id FROM kapture_dc.gms3_dmt_es_mx_cat
UNION ALL
SELECT 'gms3_dmt_es_mx_vin' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_id) AS max_id FROM kapture_dc.gms3_dmt_es_mx_vin
UNION ALL
SELECT 'gms3_dmt_es_mx_vinattr' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_attr_id) AS max_id FROM kapture_dc.gms3_dmt_es_mx_vinattr
UNION ALL
SELECT 'gms3_dmt_es_mx_vinmaster' AS table_name, COUNT(*) AS row_count, MAX(dc_vm_id) AS max_id FROM kapture_dc.gms3_dmt_es_mx_vinmaster
UNION ALL
SELECT 'gms3_dmt_es_mx_cvc' AS table_name, COUNT(*) AS row_count, MAX(dc_cvc_id) AS max_id FROM kapture_dc.gms3_dmt_es_mx_cvc
UNION ALL
SELECT 'gms3_dmt_es_mx_inrlks' AS table_name, COUNT(*) AS row_count, MAX(dc_il_id) AS max_id FROM kapture_dc.gms3_dmt_es_mx_inrlks
UNION ALL
SELECT 'gms3_dmt_fr_ca_imdoc' AS table_name, COUNT(*) AS row_count, MAX(dc_dd_id) AS max_id FROM kapture_dc.gms3_dmt_fr_ca_imdoc
UNION ALL
SELECT 'gms3_dmt_fr_ca_cat' AS table_name, COUNT(*) AS row_count, MAX(dc_cd_id) AS max_id FROM kapture_dc.gms3_dmt_fr_ca_cat
UNION ALL
SELECT 'gms3_dmt_fr_ca_vin' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_id) AS max_id FROM kapture_dc.gms3_dmt_fr_ca_vin
UNION ALL
SELECT 'gms3_dmt_fr_ca_vinattr' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_attr_id) AS max_id FROM kapture_dc.gms3_dmt_fr_ca_vinattr
UNION ALL
SELECT 'gms3_dmt_fr_ca_vinmaster' AS table_name, COUNT(*) AS row_count, MAX(dc_vm_id) AS max_id FROM kapture_dc.gms3_dmt_fr_ca_vinmaster
UNION ALL
SELECT 'gms3_dmt_fr_ca_cvc' AS table_name, COUNT(*) AS row_count, MAX(dc_cvc_id) AS max_id FROM kapture_dc.gms3_dmt_fr_ca_cvc
UNION ALL
SELECT 'gms3_dmt_fr_ca_inrlks' AS table_name, COUNT(*) AS row_count, MAX(dc_il_id) AS max_id FROM kapture_dc.gms3_dmt_fr_ca_inrlks
UNION ALL
SELECT 'gms3_dmt_en_ca_imdoc' AS table_name, COUNT(*) AS row_count, MAX(dc_dd_id) AS max_id FROM kapture_dc.gms3_dmt_en_ca_imdoc
UNION ALL
SELECT 'gms3_dmt_en_ca_cat' AS table_name, COUNT(*) AS row_count, MAX(dc_cd_id) AS max_id FROM kapture_dc.gms3_dmt_en_ca_cat
UNION ALL
SELECT 'gms3_dmt_en_ca_vin' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_id) AS max_id FROM kapture_dc.gms3_dmt_en_ca_vin
UNION ALL
SELECT 'gms3_dmt_en_ca_vinattr' AS table_name, COUNT(*) AS row_count, MAX(dc_vin_attr_id) AS max_id FROM kapture_dc.gms3_dmt_en_ca_vinattr
UNION ALL
SELECT 'gms3_dmt_en_ca_vinmaster' AS table_name, COUNT(*) AS row_count, MAX(dc_vm_id) AS max_id FROM kapture_dc.gms3_dmt_en_ca_vinmaster
UNION ALL
SELECT 'gms3_dmt_en_ca_cvc' AS table_name, COUNT(*) AS row_count, MAX(dc_cvc_id) AS max_id FROM kapture_dc.gms3_dmt_en_ca_cvc
UNION ALL
SELECT 'gms3_dmt_en_ca_inrlks' AS table_name, COUNT(*) AS row_count, MAX(dc_il_id) AS max_id FROM kapture_dc.gms3_dmt_en_ca_inrlks
UNION ALL
SELECT 'gms3_vc_vin_details', COUNT(*), MAX(vc_vin_id) FROM kapture_dc.gms3_vc_vin_details WHERE vc_vin_locale IN ('en_US','es_MX','fr_CA','en_CA')
UNION ALL
SELECT 'gms3_vc_model_year_details', COUNT(*), MAX(vc_my_id) FROM kapture_dc.gms3_vc_model_year_details WHERE vc_my_locale IN ('en_US','es_MX','fr_CA','en_CA');


-- ---------------------------------------------------------------------------------------------
-- A1. All markets: dc_load_type on the schedule table (MASTER_DATA_WITH_CONTENT or ONLY_CONTENT;
--     NULL = ONLY_CONTENT)
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_dmt_conv_schedule
  ADD COLUMN dc_load_type VARCHAR(30) NULL, ALGORITHM=INSTANT;

-- ---------------------------------------------------------------------------------------------
-- A1. All markets: content preview - one row per document per job (used by the Publish Content job)
--     pv_status: ACTIVE (written, not published yet) or DELETED (unpublished by the later job
--     pv_deleted_schedule_id). Indexes: primary key pv_id; unique u1 (job + document); n1 (document id)
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_preview_docs (
	pv_id                 BIGINT        NOT NULL AUTO_INCREMENT,
	pv_schedule_id        BIGINT        NOT NULL,
	pv_locale             VARCHAR(20)   NULL,
	pv_document_id        VARCHAR(50)   NOT NULL,
	pv_document_version   VARCHAR(20)   NULL,
	pv_row_id             VARCHAR(50)   NULL,
	pv_channel            VARCHAR(100)  NULL,
	pv_document_type      VARCHAR(30)   NULL,
	pv_model_type         VARCHAR(20)   NULL,
	pv_model_folder       VARCHAR(200)  NULL,
	pv_manual_type        VARCHAR(100)  NULL,
	pv_title              VARCHAR(4000) NULL,
	pv_source_network_loc VARCHAR(512)  NULL,
	pv_status             VARCHAR(20)   NOT NULL,
	pv_created_tmstp      DATETIME(6)   NULL,
	pv_updated_tmstp      DATETIME(6)   NULL,
	PRIMARY KEY (pv_id),
	UNIQUE KEY gms3_dmt_preview_docs_u1 (pv_schedule_id, pv_document_id),
	KEY gms3_dmt_preview_docs_n1 (pv_document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- the job whose delete processing unpublished the document
ALTER TABLE kapture_dc.gms3_dmt_preview_docs ADD COLUMN pv_deleted_schedule_id BIGINT NULL, ALGORITHM=INSTANT;

-- ---------------------------------------------------------------------------------------------
-- A2. MC: vc_content_status on the view content table
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_vc_japan_vin_details
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_japan_vin_details
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- ---------------------------------------------------------------------------------------------
-- A3. MME: vc_content_status on the 14 view content tables
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_de_de
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_de_de
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_el_gr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_el_gr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_en_uk
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_en_uk
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_es_es
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_es_es
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_it_it
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_it_it
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_sv_se
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_sv_se
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- ---------------------------------------------------------------------------------------------
-- A4. MNAO (en_US, es_MX, fr_CA, en_CA)
-- ---------------------------------------------------------------------------------------------
-- A4.1 dc_facelift_folder_name on the MNAO document tables
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_dmt_en_us_imdoc
  ADD COLUMN dc_facelift_folder_name VARCHAR(50) NULL, ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_dmt_es_mx_imdoc
  ADD COLUMN dc_facelift_folder_name VARCHAR(50) NULL, ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_dmt_fr_ca_imdoc
  ADD COLUMN dc_facelift_folder_name VARCHAR(50) NULL, ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_dmt_en_ca_imdoc
  ADD COLUMN dc_facelift_folder_name VARCHAR(50) NULL, ALGORITHM=INSTANT;

-- ---------------------------------------------------------------------------------------------
-- A4.2 New tables: display order (d01.txt ... rows) and esicat (esicat.txt rows) per MNAO locale
--    Same columns as the MME tables gms3_dmt_en_uk_nm_dispord / gms3_dmt_en_uk_nm_esicat.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_en_us_dispord (
	dc_dispord_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_dispord_level_1 VARCHAR(50) NULL,
	dc_dispord_level_2 VARCHAR(50) NULL,
	dc_dispord_level_3 VARCHAR(50) NULL,
	dc_dispord_level_4 VARCHAR(50) NULL,
	dc_dispord_level_5 VARCHAR(50) NULL,
	dc_dispord_level_6 VARCHAR(50) NULL,
	dc_dispord_level_7 VARCHAR(50) NULL,
	dc_dispord_level_8 VARCHAR(50) NULL,
	dc_dispord_model_folder VARCHAR(100) NULL,
	dc_dispord_manualtype_folder VARCHAR(100) NULL,
	dc_dispord_facelift_folder VARCHAR(50) NULL,
	dc_dispord_material_folder VARCHAR(100) NULL,
	dc_dispord_filetype_folder VARCHAR(50) NULL,
	dc_dispord_source_file_name VARCHAR(100) NULL,
	dc_im_doc_manual_type_refkey VARCHAR(200) NULL,
	dc_dispord_txt_file_name VARCHAR(150) NULL,
	dc_dispord_txt_file_path VARCHAR(256) NULL,
	dc_sequence_no VARCHAR(50) NULL,
	dc_engine_type VARCHAR(200) NULL,
	dc_mission_type VARCHAR(200) NULL,
	dc_driveaxle_type VARCHAR(200) NULL,
	dc_body_type VARCHAR(200) NULL,
	dc_title VARCHAR(4000) NULL,
	dc_dispord_name_level_1 VARCHAR(1000) NULL,
	dc_dispord_name_level_2 VARCHAR(1000) NULL,
	dc_dispord_name_level_3 VARCHAR(1000) NULL,
	dc_dispord_name_level_4 VARCHAR(1000) NULL,
	dc_dispord_name_level_5 VARCHAR(1000) NULL,
	dc_dispord_name_level_6 VARCHAR(1000) NULL,
	dc_dispord_name_level_7 VARCHAR(1000) NULL,
	dc_dispord_name_level_8 VARCHAR(1000) NULL,
	dc_dispord_created_tmstp DATETIME(6) NULL,
	dc_dispord_updated_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_dispord_id),
	KEY gms3_dmt_en_us_dispord_n1 (dc_im_doc_id),
	KEY gms3_dmt_en_us_dispord_fk1 (dc_schedule_id),
	KEY gms3_dmt_en_us_dispord_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_en_us_dispord_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_en_us_esicat (
	dc_esicat_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_esicat_code_level_1 VARCHAR(20) NULL,
	dc_esicat_code_level_2 VARCHAR(20) NULL,
	dc_esicat_code_level_3 VARCHAR(20) NULL,
	dc_esicat_mapped_level VARCHAR(50) NULL,
	dc_esicat_mapped_refkey VARCHAR(50) NULL,
	dc_esicat_txt_file_name VARCHAR(150) NULL,
	dc_esicat_txt_file_path VARCHAR(256) NULL,
	dc_steering_type_info VARCHAR(100) NULL,
	dc_esicat_created_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_esicat_id),
	KEY gms3_dmt_en_us_esicat_n1 (dc_im_doc_id),
	KEY gms3_dmt_en_us_esicat_fk1 (dc_schedule_id),
	KEY gms3_dmt_en_us_esicat_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_en_us_esicat_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_es_mx_dispord (
	dc_dispord_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_dispord_level_1 VARCHAR(50) NULL,
	dc_dispord_level_2 VARCHAR(50) NULL,
	dc_dispord_level_3 VARCHAR(50) NULL,
	dc_dispord_level_4 VARCHAR(50) NULL,
	dc_dispord_level_5 VARCHAR(50) NULL,
	dc_dispord_level_6 VARCHAR(50) NULL,
	dc_dispord_level_7 VARCHAR(50) NULL,
	dc_dispord_level_8 VARCHAR(50) NULL,
	dc_dispord_model_folder VARCHAR(100) NULL,
	dc_dispord_manualtype_folder VARCHAR(100) NULL,
	dc_dispord_facelift_folder VARCHAR(50) NULL,
	dc_dispord_material_folder VARCHAR(100) NULL,
	dc_dispord_filetype_folder VARCHAR(50) NULL,
	dc_dispord_source_file_name VARCHAR(100) NULL,
	dc_im_doc_manual_type_refkey VARCHAR(200) NULL,
	dc_dispord_txt_file_name VARCHAR(150) NULL,
	dc_dispord_txt_file_path VARCHAR(256) NULL,
	dc_sequence_no VARCHAR(50) NULL,
	dc_engine_type VARCHAR(200) NULL,
	dc_mission_type VARCHAR(200) NULL,
	dc_driveaxle_type VARCHAR(200) NULL,
	dc_body_type VARCHAR(200) NULL,
	dc_title VARCHAR(4000) NULL,
	dc_dispord_name_level_1 VARCHAR(1000) NULL,
	dc_dispord_name_level_2 VARCHAR(1000) NULL,
	dc_dispord_name_level_3 VARCHAR(1000) NULL,
	dc_dispord_name_level_4 VARCHAR(1000) NULL,
	dc_dispord_name_level_5 VARCHAR(1000) NULL,
	dc_dispord_name_level_6 VARCHAR(1000) NULL,
	dc_dispord_name_level_7 VARCHAR(1000) NULL,
	dc_dispord_name_level_8 VARCHAR(1000) NULL,
	dc_dispord_created_tmstp DATETIME(6) NULL,
	dc_dispord_updated_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_dispord_id),
	KEY gms3_dmt_es_mx_dispord_n1 (dc_im_doc_id),
	KEY gms3_dmt_es_mx_dispord_fk1 (dc_schedule_id),
	KEY gms3_dmt_es_mx_dispord_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_es_mx_dispord_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_es_mx_esicat (
	dc_esicat_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_esicat_code_level_1 VARCHAR(20) NULL,
	dc_esicat_code_level_2 VARCHAR(20) NULL,
	dc_esicat_code_level_3 VARCHAR(20) NULL,
	dc_esicat_mapped_level VARCHAR(50) NULL,
	dc_esicat_mapped_refkey VARCHAR(50) NULL,
	dc_esicat_txt_file_name VARCHAR(150) NULL,
	dc_esicat_txt_file_path VARCHAR(256) NULL,
	dc_steering_type_info VARCHAR(100) NULL,
	dc_esicat_created_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_esicat_id),
	KEY gms3_dmt_es_mx_esicat_n1 (dc_im_doc_id),
	KEY gms3_dmt_es_mx_esicat_fk1 (dc_schedule_id),
	KEY gms3_dmt_es_mx_esicat_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_es_mx_esicat_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_fr_ca_dispord (
	dc_dispord_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_dispord_level_1 VARCHAR(50) NULL,
	dc_dispord_level_2 VARCHAR(50) NULL,
	dc_dispord_level_3 VARCHAR(50) NULL,
	dc_dispord_level_4 VARCHAR(50) NULL,
	dc_dispord_level_5 VARCHAR(50) NULL,
	dc_dispord_level_6 VARCHAR(50) NULL,
	dc_dispord_level_7 VARCHAR(50) NULL,
	dc_dispord_level_8 VARCHAR(50) NULL,
	dc_dispord_model_folder VARCHAR(100) NULL,
	dc_dispord_manualtype_folder VARCHAR(100) NULL,
	dc_dispord_facelift_folder VARCHAR(50) NULL,
	dc_dispord_material_folder VARCHAR(100) NULL,
	dc_dispord_filetype_folder VARCHAR(50) NULL,
	dc_dispord_source_file_name VARCHAR(100) NULL,
	dc_im_doc_manual_type_refkey VARCHAR(200) NULL,
	dc_dispord_txt_file_name VARCHAR(150) NULL,
	dc_dispord_txt_file_path VARCHAR(256) NULL,
	dc_sequence_no VARCHAR(50) NULL,
	dc_engine_type VARCHAR(200) NULL,
	dc_mission_type VARCHAR(200) NULL,
	dc_driveaxle_type VARCHAR(200) NULL,
	dc_body_type VARCHAR(200) NULL,
	dc_title VARCHAR(4000) NULL,
	dc_dispord_name_level_1 VARCHAR(1000) NULL,
	dc_dispord_name_level_2 VARCHAR(1000) NULL,
	dc_dispord_name_level_3 VARCHAR(1000) NULL,
	dc_dispord_name_level_4 VARCHAR(1000) NULL,
	dc_dispord_name_level_5 VARCHAR(1000) NULL,
	dc_dispord_name_level_6 VARCHAR(1000) NULL,
	dc_dispord_name_level_7 VARCHAR(1000) NULL,
	dc_dispord_name_level_8 VARCHAR(1000) NULL,
	dc_dispord_created_tmstp DATETIME(6) NULL,
	dc_dispord_updated_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_dispord_id),
	KEY gms3_dmt_fr_ca_dispord_n1 (dc_im_doc_id),
	KEY gms3_dmt_fr_ca_dispord_fk1 (dc_schedule_id),
	KEY gms3_dmt_fr_ca_dispord_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_fr_ca_dispord_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_fr_ca_esicat (
	dc_esicat_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_esicat_code_level_1 VARCHAR(20) NULL,
	dc_esicat_code_level_2 VARCHAR(20) NULL,
	dc_esicat_code_level_3 VARCHAR(20) NULL,
	dc_esicat_mapped_level VARCHAR(50) NULL,
	dc_esicat_mapped_refkey VARCHAR(50) NULL,
	dc_esicat_txt_file_name VARCHAR(150) NULL,
	dc_esicat_txt_file_path VARCHAR(256) NULL,
	dc_steering_type_info VARCHAR(100) NULL,
	dc_esicat_created_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_esicat_id),
	KEY gms3_dmt_fr_ca_esicat_n1 (dc_im_doc_id),
	KEY gms3_dmt_fr_ca_esicat_fk1 (dc_schedule_id),
	KEY gms3_dmt_fr_ca_esicat_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_fr_ca_esicat_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_en_ca_dispord (
	dc_dispord_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_dispord_level_1 VARCHAR(50) NULL,
	dc_dispord_level_2 VARCHAR(50) NULL,
	dc_dispord_level_3 VARCHAR(50) NULL,
	dc_dispord_level_4 VARCHAR(50) NULL,
	dc_dispord_level_5 VARCHAR(50) NULL,
	dc_dispord_level_6 VARCHAR(50) NULL,
	dc_dispord_level_7 VARCHAR(50) NULL,
	dc_dispord_level_8 VARCHAR(50) NULL,
	dc_dispord_model_folder VARCHAR(100) NULL,
	dc_dispord_manualtype_folder VARCHAR(100) NULL,
	dc_dispord_facelift_folder VARCHAR(50) NULL,
	dc_dispord_material_folder VARCHAR(100) NULL,
	dc_dispord_filetype_folder VARCHAR(50) NULL,
	dc_dispord_source_file_name VARCHAR(100) NULL,
	dc_im_doc_manual_type_refkey VARCHAR(200) NULL,
	dc_dispord_txt_file_name VARCHAR(150) NULL,
	dc_dispord_txt_file_path VARCHAR(256) NULL,
	dc_sequence_no VARCHAR(50) NULL,
	dc_engine_type VARCHAR(200) NULL,
	dc_mission_type VARCHAR(200) NULL,
	dc_driveaxle_type VARCHAR(200) NULL,
	dc_body_type VARCHAR(200) NULL,
	dc_title VARCHAR(4000) NULL,
	dc_dispord_name_level_1 VARCHAR(1000) NULL,
	dc_dispord_name_level_2 VARCHAR(1000) NULL,
	dc_dispord_name_level_3 VARCHAR(1000) NULL,
	dc_dispord_name_level_4 VARCHAR(1000) NULL,
	dc_dispord_name_level_5 VARCHAR(1000) NULL,
	dc_dispord_name_level_6 VARCHAR(1000) NULL,
	dc_dispord_name_level_7 VARCHAR(1000) NULL,
	dc_dispord_name_level_8 VARCHAR(1000) NULL,
	dc_dispord_created_tmstp DATETIME(6) NULL,
	dc_dispord_updated_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_dispord_id),
	KEY gms3_dmt_en_ca_dispord_n1 (dc_im_doc_id),
	KEY gms3_dmt_en_ca_dispord_fk1 (dc_schedule_id),
	KEY gms3_dmt_en_ca_dispord_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_en_ca_dispord_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kapture_dc.gms3_dmt_en_ca_esicat (
	dc_esicat_id BIGINT NOT NULL AUTO_INCREMENT,
	dc_schedule_id BIGINT NOT NULL,
	dc_im_doc_id VARCHAR(50) NOT NULL,
	dc_source_network_loc VARCHAR(256) NULL,
	dc_esicat_code_level_1 VARCHAR(20) NULL,
	dc_esicat_code_level_2 VARCHAR(20) NULL,
	dc_esicat_code_level_3 VARCHAR(20) NULL,
	dc_esicat_mapped_level VARCHAR(50) NULL,
	dc_esicat_mapped_refkey VARCHAR(50) NULL,
	dc_esicat_txt_file_name VARCHAR(150) NULL,
	dc_esicat_txt_file_path VARCHAR(256) NULL,
	dc_steering_type_info VARCHAR(100) NULL,
	dc_esicat_created_tmstp DATETIME(6) NULL,
	PRIMARY KEY (dc_esicat_id),
	KEY gms3_dmt_en_ca_esicat_n1 (dc_im_doc_id),
	KEY gms3_dmt_en_ca_esicat_fk1 (dc_schedule_id),
	KEY gms3_dmt_en_ca_esicat_n2 ((TRIM(LOWER(dc_source_network_loc)))),
	CONSTRAINT gms3_dmt_en_ca_esicat_fk1 FOREIGN KEY (dc_schedule_id) REFERENCES kapture_dc.gms3_dmt_conv_schedule (dc_schedule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------------------------
-- A4.3 vc_content_status on the MNAO view content tables (every existing row reads 'Published')
--    Values configured in application.properties: view.content.status.draft / .published
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_vc_vin_details
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_vin_details
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
ALTER TABLE kapture_dc.gms3_vc_model_year_details
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_model_year_details
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- ---------------------------------------------------------------------------------------------
-- A4.4 Display order columns on the MNAO view content tables (codes stay empty, names are filled -
--    the MC rule; existing rows stay NULL)
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_vc_vin_details
  ADD COLUMN vc_vin_disp_code_1 VARCHAR(20) NULL,
  ADD COLUMN vc_vin_disp_name_1 VARCHAR(1000) NULL,
  ADD COLUMN vc_vin_disp_code_2 VARCHAR(20) NULL,
  ADD COLUMN vc_vin_disp_name_2 VARCHAR(1000) NULL,
  ADD COLUMN vc_vin_disp_code_3 VARCHAR(20) NULL,
  ADD COLUMN vc_vin_disp_name_3 VARCHAR(1000) NULL,
  ADD COLUMN vc_vin_disp_code_4 VARCHAR(20) NULL,
  ADD COLUMN vc_vin_disp_name_4 VARCHAR(1000) NULL,
  ADD COLUMN vc_vin_disp_code_5 VARCHAR(20) NULL,
  ADD COLUMN vc_vin_disp_name_5 VARCHAR(1000) NULL,
  ADD COLUMN vc_vin_disp_code_6 VARCHAR(20) NULL,
  ADD COLUMN vc_vin_disp_name_6 VARCHAR(1000) NULL,
  ADD COLUMN vc_vin_dispord_seq_no VARCHAR(20) NULL,
  ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_model_year_details
  ADD COLUMN vc_my_disp_code_1 VARCHAR(20) NULL,
  ADD COLUMN vc_my_disp_name_1 VARCHAR(1000) NULL,
  ADD COLUMN vc_my_disp_code_2 VARCHAR(20) NULL,
  ADD COLUMN vc_my_disp_name_2 VARCHAR(1000) NULL,
  ADD COLUMN vc_my_disp_code_3 VARCHAR(20) NULL,
  ADD COLUMN vc_my_disp_name_3 VARCHAR(1000) NULL,
  ADD COLUMN vc_my_disp_code_4 VARCHAR(20) NULL,
  ADD COLUMN vc_my_disp_name_4 VARCHAR(1000) NULL,
  ADD COLUMN vc_my_disp_code_5 VARCHAR(20) NULL,
  ADD COLUMN vc_my_disp_name_5 VARCHAR(1000) NULL,
  ADD COLUMN vc_my_disp_code_6 VARCHAR(20) NULL,
  ADD COLUMN vc_my_disp_name_6 VARCHAR(1000) NULL,
  ADD COLUMN vc_my_dispord_seq_no VARCHAR(20) NULL,
  ALGORITHM=INSTANT;

-- ---------------------------------------------------------------------------------------------
-- A4.5 Source path index on gms3_dmt_en_ca_imdoc (en_us / es_mx / fr_ca have it already)
-- ---------------------------------------------------------------------------------------------
CREATE INDEX gms3_dmt_en_ca_imdoc_n2 ON kapture_dc.gms3_dmt_en_ca_imdoc ((TRIM(LOWER(dc_source_network_loc))));

-- ---------------------------------------------------------------------------------------------
-- B1. All markets: view content rows still without a status -> 'Published'
--     Finds rows only when rows were loaded after part A (e.g. Job 1 ran again) or on a re-run.
-- ---------------------------------------------------------------------------------------------
UPDATE kapture_dc.gms3_vc_japan_vin_details SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_de_de SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_el_gr SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_en_uk SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_es_es SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_it_it SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_sv_se SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr SET vc_content_status = 'Published' WHERE vc_content_status IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_vc_vin_details SET vc_content_status = 'Published' WHERE vc_content_status IS NULL AND vc_vin_locale IN ('en_US','es_MX','fr_CA','en_CA');
COMMIT;
UPDATE kapture_dc.gms3_vc_model_year_details SET vc_content_status = 'Published' WHERE vc_content_status IS NULL AND vc_my_locale IN ('en_US','es_MX','fr_CA','en_CA');
COMMIT;

-- preview rows marked WITHDRAWN by an earlier war are now DELETED (finds nothing on a new environment)
UPDATE kapture_dc.gms3_dmt_preview_docs SET pv_status = 'DELETED' WHERE pv_status = 'WITHDRAWN';
COMMIT;

-- ---------------------------------------------------------------------------------------------
-- B2. MNAO: existing document rows belong to the facelift folder FL0000
-- ---------------------------------------------------------------------------------------------
UPDATE kapture_dc.gms3_dmt_en_us_imdoc SET dc_facelift_folder_name = 'FL0000' WHERE dc_facelift_folder_name IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_dmt_es_mx_imdoc SET dc_facelift_folder_name = 'FL0000' WHERE dc_facelift_folder_name IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_dmt_fr_ca_imdoc SET dc_facelift_folder_name = 'FL0000' WHERE dc_facelift_folder_name IS NULL;
COMMIT;
UPDATE kapture_dc.gms3_dmt_en_ca_imdoc SET dc_facelift_folder_name = 'FL0000' WHERE dc_facelift_folder_name IS NULL;
COMMIT;

-- ---------------------------------------------------------------------------------------------
-- B3. MNAO: ONE-TIME PATH REWRITE - 'FL0000\' after the 3rd backslash
--       en-US\CX-5_KF\WM\<material>\ent.gms3\x.ent -> en-US\CX-5_KF\WM\FL0000\<material>\ent.gms3\x.ent
--     in gms3_dmt_<loc>_{imdoc,cat,vin,vinattr,vinmaster,cvc,inrlks} (inrlks also dc_inner_doc_link_path),
--     gms3_vc_vin_details and gms3_vc_model_year_details (MNAO locales only).
--     NOT touched: gms3_dmt_conv_*, gms3_dmt_conv_reprpse_doc(_tmp), gms3_dmt_imdoc_child_map(_t).
-- ---------------------------------------------------------------------------------------------
-- B3. kapture_dc.gms3_dmt_en_us_imdoc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_us_imdoc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_us_cat (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_us_cat SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_us_vin (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_us_vin SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_us_vinattr (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_us_vinattr SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_us_vinmaster (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_us_vinmaster SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_us_cvc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_us_cvc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_us_inrlks (dc_source_network_loc, dc_inner_doc_link_path)
UPDATE kapture_dc.gms3_dmt_en_us_inrlks SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;
UPDATE kapture_dc.gms3_dmt_en_us_inrlks SET dc_inner_doc_link_path = CONCAT(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_inner_doc_link_path, CHAR_LENGTH(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_es_mx_imdoc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_es_mx_imdoc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_es_mx_cat (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_es_mx_cat SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_es_mx_vin (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_es_mx_vin SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_es_mx_vinattr (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_es_mx_vinattr SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_es_mx_vinmaster (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_es_mx_vinmaster SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_es_mx_cvc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_es_mx_cvc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_es_mx_inrlks (dc_source_network_loc, dc_inner_doc_link_path)
UPDATE kapture_dc.gms3_dmt_es_mx_inrlks SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;
UPDATE kapture_dc.gms3_dmt_es_mx_inrlks SET dc_inner_doc_link_path = CONCAT(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_inner_doc_link_path, CHAR_LENGTH(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_fr_ca_imdoc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_fr_ca_imdoc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_fr_ca_cat (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_fr_ca_cat SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_fr_ca_vin (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_fr_ca_vin SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_fr_ca_vinattr (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_fr_ca_vinattr SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_fr_ca_vinmaster (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_fr_ca_vinmaster SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_fr_ca_cvc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_fr_ca_cvc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_fr_ca_inrlks (dc_source_network_loc, dc_inner_doc_link_path)
UPDATE kapture_dc.gms3_dmt_fr_ca_inrlks SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;
UPDATE kapture_dc.gms3_dmt_fr_ca_inrlks SET dc_inner_doc_link_path = CONCAT(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_inner_doc_link_path, CHAR_LENGTH(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_ca_imdoc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_ca_imdoc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_ca_cat (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_ca_cat SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_ca_vin (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_ca_vin SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_ca_vinattr (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_ca_vinattr SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_ca_vinmaster (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_ca_vinmaster SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_ca_cvc (dc_source_network_loc)
UPDATE kapture_dc.gms3_dmt_en_ca_cvc SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_dmt_en_ca_inrlks (dc_source_network_loc, dc_inner_doc_link_path)
UPDATE kapture_dc.gms3_dmt_en_ca_inrlks SET dc_source_network_loc = CONCAT(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_source_network_loc, CHAR_LENGTH(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;
UPDATE kapture_dc.gms3_dmt_en_ca_inrlks SET dc_inner_doc_link_path = CONCAT(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(dc_inner_doc_link_path, CHAR_LENGTH(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 3)) + 2)) WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_vc_vin_details.vc_vin_source_ntwrk_loc (MNAO locales)
UPDATE kapture_dc.gms3_vc_vin_details SET vc_vin_source_ntwrk_loc = CONCAT(SUBSTRING_INDEX(vc_vin_source_ntwrk_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(vc_vin_source_ntwrk_loc, CHAR_LENGTH(SUBSTRING_INDEX(vc_vin_source_ntwrk_loc, CHAR(92), 3)) + 2)) WHERE vc_vin_locale IN ('en_US','es_MX','fr_CA','en_CA') AND ((LENGTH(vc_vin_source_ntwrk_loc) - LENGTH(REPLACE(vc_vin_source_ntwrk_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(vc_vin_source_ntwrk_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- B3. kapture_dc.gms3_vc_model_year_details.vc_my_source_ntwrk_loc (MNAO locales)
UPDATE kapture_dc.gms3_vc_model_year_details SET vc_my_source_ntwrk_loc = CONCAT(SUBSTRING_INDEX(vc_my_source_ntwrk_loc, CHAR(92), 3), CHAR(92), 'FL0000', CHAR(92), SUBSTRING(vc_my_source_ntwrk_loc, CHAR_LENGTH(SUBSTRING_INDEX(vc_my_source_ntwrk_loc, CHAR(92), 3)) + 2)) WHERE vc_my_locale IN ('en_US','es_MX','fr_CA','en_CA') AND ((LENGTH(vc_my_source_ntwrk_loc) - LENGTH(REPLACE(vc_my_source_ntwrk_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(vc_my_source_ntwrk_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');
COMMIT;

-- ---------------------------------------------------------------------------------------------
-- C. Checks (read only)
-- ---------------------------------------------------------------------------------------------
-- C1. New columns - expected: every row listed, default NULL (dc_facelift_folder_name /
--     display order columns NULL default as well)
SELECT table_name, column_name, column_type, column_default
  FROM information_schema.columns
 WHERE table_schema = 'kapture_dc'
   AND ((table_name = 'gms3_dmt_conv_schedule' AND column_name = 'dc_load_type')
     OR (table_name = 'gms3_dmt_preview_docs' AND column_name = 'pv_deleted_schedule_id')
     OR (table_name = 'gms3_vc_japan_vin_details' AND column_name = 'vc_content_status')
     OR (table_name LIKE 'gms3!_vc!_mme!_vin!_dtl!_%' ESCAPE '!' AND column_name = 'vc_content_status')
     OR (table_name IN ('gms3_dmt_en_us_imdoc','gms3_dmt_es_mx_imdoc','gms3_dmt_fr_ca_imdoc','gms3_dmt_en_ca_imdoc')
         AND column_name = 'dc_facelift_folder_name')
     OR (table_name IN ('gms3_vc_vin_details','gms3_vc_model_year_details')
         AND (column_name = 'vc_content_status' OR column_name LIKE '%disp%')))
 ORDER BY table_name, ordinal_position;

-- C2. New tables - expected 9 rows
SELECT table_name, table_rows
  FROM information_schema.tables
 WHERE table_schema = 'kapture_dc'
   AND table_name IN ('gms3_dmt_preview_docs',
                      'gms3_dmt_en_us_dispord','gms3_dmt_es_mx_dispord','gms3_dmt_fr_ca_dispord','gms3_dmt_en_ca_dispord',
                      'gms3_dmt_en_us_esicat','gms3_dmt_es_mx_esicat','gms3_dmt_fr_ca_esicat','gms3_dmt_en_ca_esicat')
 ORDER BY table_name;

-- C3. Index on gms3_dmt_en_ca_imdoc - expected gms3_dmt_en_ca_imdoc_n2 listed
SELECT DISTINCT index_name FROM information_schema.statistics
 WHERE table_schema = 'kapture_dc' AND table_name = 'gms3_dmt_en_ca_imdoc' AND index_name = 'gms3_dmt_en_ca_imdoc_n2';

-- C4. View content rows without a status - expected 0 on every line
SELECT 'gms3_vc_japan_vin_details' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_japan_vin_details WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_cs_cz' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_cs_cz WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_de_de' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_de_de WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_el_gr' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_el_gr WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_en_uk' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_en_uk WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_es_es' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_es_es WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_fi_fi' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_fi_fi WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_fr_fr' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_fr_fr WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_it_it' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_it_it WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_nl_nl' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_nl_nl WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_pl_pl' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_pl_pl WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_pt_pt' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_pt_pt WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_ru_ru' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_ru_ru WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_sv_se' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_sv_se WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_mme_vin_dtl_tr_tr' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_mme_vin_dtl_tr_tr WHERE vc_content_status IS NULL
UNION ALL
SELECT 'gms3_vc_vin_details' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_vin_details WHERE vc_content_status IS NULL AND vc_vin_locale IN ('en_US','es_MX','fr_CA','en_CA')
UNION ALL
SELECT 'gms3_vc_model_year_details' AS table_name, COUNT(*) AS without_status FROM kapture_dc.gms3_vc_model_year_details WHERE vc_content_status IS NULL AND vc_my_locale IN ('en_US','es_MX','fr_CA','en_CA');

-- C5. Paths still WITHOUT the facelift folder - expected 0 on every line
SELECT 'gms3_dmt_en_us_imdoc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_imdoc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_us_cat.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_cat WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_us_vin.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_vin WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_us_vinattr.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_vinattr WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_us_vinmaster.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_vinmaster WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_us_cvc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_cvc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_us_inrlks.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_inrlks WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_us_inrlks.dc_inner_doc_link_path' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_us_inrlks WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_imdoc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_imdoc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_cat.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_cat WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_vin.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_vin WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_vinattr.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_vinattr WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_vinmaster.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_vinmaster WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_cvc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_cvc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_inrlks.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_inrlks WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_es_mx_inrlks.dc_inner_doc_link_path' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_es_mx_inrlks WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_imdoc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_imdoc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_cat.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_cat WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_vin.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_vin WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_vinattr.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_vinattr WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_vinmaster.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_vinmaster WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_cvc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_cvc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_inrlks.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_inrlks WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_fr_ca_inrlks.dc_inner_doc_link_path' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_fr_ca_inrlks WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_imdoc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_imdoc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_cat.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_cat WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_vin.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_vin WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_vinattr.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_vinattr WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_vinmaster.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_vinmaster WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_cvc.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_cvc WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_inrlks.dc_source_network_loc' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_inrlks WHERE ((LENGTH(dc_source_network_loc) - LENGTH(REPLACE(dc_source_network_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_source_network_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_dmt_en_ca_inrlks.dc_inner_doc_link_path' AS path_column, COUNT(*) AS without_facelift FROM kapture_dc.gms3_dmt_en_ca_inrlks WHERE ((LENGTH(dc_inner_doc_link_path) - LENGTH(REPLACE(dc_inner_doc_link_path, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(dc_inner_doc_link_path, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_vc_vin_details.vc_vin_source_ntwrk_loc', COUNT(*) FROM kapture_dc.gms3_vc_vin_details WHERE vc_vin_locale IN ('en_US','es_MX','fr_CA','en_CA') AND ((LENGTH(vc_vin_source_ntwrk_loc) - LENGTH(REPLACE(vc_vin_source_ntwrk_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(vc_vin_source_ntwrk_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$')
UNION ALL
SELECT 'gms3_vc_model_year_details.vc_my_source_ntwrk_loc', COUNT(*) FROM kapture_dc.gms3_vc_model_year_details WHERE vc_my_locale IN ('en_US','es_MX','fr_CA','en_CA') AND ((LENGTH(vc_my_source_ntwrk_loc) - LENGTH(REPLACE(vc_my_source_ntwrk_loc, CHAR(92), ''))) >= 4 AND SUBSTRING_INDEX(SUBSTRING_INDEX(vc_my_source_ntwrk_loc, CHAR(92), 4), CHAR(92), -1) NOT REGEXP '^FL[0-9]{4}$');

-- C6. Document rows per facelift folder - expected FL0000 only (until the first new MNAO job)
SELECT 'gms3_dmt_en_us_imdoc' AS table_name, dc_facelift_folder_name, COUNT(*) AS row_count FROM kapture_dc.gms3_dmt_en_us_imdoc GROUP BY dc_facelift_folder_name
UNION ALL
SELECT 'gms3_dmt_es_mx_imdoc' AS table_name, dc_facelift_folder_name, COUNT(*) AS row_count FROM kapture_dc.gms3_dmt_es_mx_imdoc GROUP BY dc_facelift_folder_name
UNION ALL
SELECT 'gms3_dmt_fr_ca_imdoc' AS table_name, dc_facelift_folder_name, COUNT(*) AS row_count FROM kapture_dc.gms3_dmt_fr_ca_imdoc GROUP BY dc_facelift_folder_name
UNION ALL
SELECT 'gms3_dmt_en_ca_imdoc' AS table_name, dc_facelift_folder_name, COUNT(*) AS row_count FROM kapture_dc.gms3_dmt_en_ca_imdoc GROUP BY dc_facelift_folder_name;

-- C7. Sample of rewritten paths
SELECT dc_source_network_loc FROM kapture_dc.gms3_dmt_en_us_imdoc ORDER BY dc_dd_id DESC LIMIT 5;
