-- ---------------------------------------------------------------------------------------------
-- DMT (dmtgt) - DDL REFERENCE: every CREATE TABLE and ALTER TABLE statement made for DMT
-- ---------------------------------------------------------------------------------------------
-- Schema: kapture_dc (change the prefix if the target environment uses another schema name).
-- MySQL 8.0. DDL only - the data steps that go with these statements stay in the original
-- deployment scripts (db/dmtgt_pre_deployment.sql, db/dmtgt_preview_docs.sql,
-- db/dmtgt_publish.sql, db/dmtgt_mme_pre_deployment.sql, db/dmtgt_mnao_pre_deployment.sql).
--
-- Order of the sections = order of execution.
--   1. gms3_vc_japan_vin_details   new column vc_content_status (MC view content)
--   2. gms3_dmt_conv_schedule      new column dc_load_type
--   3. gms3_dmt_preview_docs       new table (documents written by each job)
--   4. gms3_dmt_preview_docs       new column pv_deleted_schedule_id
--   5. gms3_vc_mme_vin_dtl_<loc>   new column vc_content_status on the 14 MME view content tables
--   6. MNAO (en_us, es_mx, fr_ca, en_ca): dc_facelift_folder_name on gms3_dmt_<loc>_imdoc, new tables
--      gms3_dmt_<loc>_dispord / gms3_dmt_<loc>_esicat, vc_content_status + display order columns on
--      gms3_vc_vin_details / gms3_vc_model_year_details, source path index on gms3_dmt_en_ca_imdoc
--
-- vc_content_status: added with DEFAULT 'Published' and ALGORITHM=INSTANT, so every existing row
-- reads 'Published' without a table rewrite; the default is then set to NULL for new rows
-- (the application writes 'Draft', the Publish Content job sets 'Published').
-- All ADD COLUMN statements use ALGORITHM=INSTANT (metadata change only, no table copy).
--
-- INDEXES: one index on an existing table - gms3_dmt_en_ca_imdoc_n2 (section 6.5, the source path index
-- the other MNAO locales already have). The other new indexes are the three on the
-- new table gms3_dmt_preview_docs, declared inside its CREATE TABLE (section 3):
--   PRIMARY KEY               (pv_id)
--   gms3_dmt_preview_docs_u1  UNIQUE (pv_schedule_id, pv_document_id)
--   gms3_dmt_preview_docs_n1  (pv_document_id)
-- ---------------------------------------------------------------------------------------------

-- ---------------------------------------------------------------------------------------------
-- 1. MC view content: vc_content_status
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_vc_japan_vin_details
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_japan_vin_details
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- ---------------------------------------------------------------------------------------------
-- 2. Schedule: dc_load_type (MASTER_DATA_WITH_CONTENT or ONLY_CONTENT; NULL = ONLY_CONTENT)
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_dmt_conv_schedule
  ADD COLUMN dc_load_type VARCHAR(30) NULL, ALGORITHM=INSTANT;

-- ---------------------------------------------------------------------------------------------
-- 3. Content preview: one row per document per job (used by the Publish Content job)
--    pv_status: ACTIVE (written, not published yet) or DELETED (unpublished by a later job)
--    Indexes: primary key pv_id; unique u1 (job + document); n1 (document id)
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

-- ---------------------------------------------------------------------------------------------
-- 4. Content preview: the job whose delete processing unpublished the document
-- ---------------------------------------------------------------------------------------------
ALTER TABLE kapture_dc.gms3_dmt_preview_docs
  ADD COLUMN pv_deleted_schedule_id BIGINT NULL, ALGORITHM=INSTANT;

-- ---------------------------------------------------------------------------------------------
-- 5. MME view content: vc_content_status on the 14 locale tables
-- ---------------------------------------------------------------------------------------------

-- 5.1 kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.2 kapture_dc.gms3_vc_mme_vin_dtl_de_de
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_de_de
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_de_de
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.3 kapture_dc.gms3_vc_mme_vin_dtl_el_gr
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_el_gr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_el_gr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.4 kapture_dc.gms3_vc_mme_vin_dtl_en_uk
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_en_uk
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_en_uk
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.5 kapture_dc.gms3_vc_mme_vin_dtl_es_es
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_es_es
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_es_es
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.6 kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.7 kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.8 kapture_dc.gms3_vc_mme_vin_dtl_it_it
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_it_it
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_it_it
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.9 kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.10 kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.11 kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.12 kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.13 kapture_dc.gms3_vc_mme_vin_dtl_sv_se
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_sv_se
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_sv_se
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 5.14 kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- ---------------------------------------------------------------------------------------------
-- 6. MNAO (db/dmtgt_mnao_pre_deployment.sql sections 1-5; its data steps 6-8 - FL0000 and the
--    one-time path rewrite - stay in that script)
-- ---------------------------------------------------------------------------------------------
-- ---------------------------------------------------------------------------------------------
-- 6.1 dc_facelift_folder_name on the MNAO document tables
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
-- 6.2 New tables: display order (d01.txt ... rows) and esicat (esicat.txt rows) per MNAO locale
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
-- 6.3 vc_content_status on the MNAO view content tables (every existing row reads 'Published')
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
-- rows still without a status (only when the script is run again, or the column existed already)
UPDATE kapture_dc.gms3_vc_vin_details SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL AND vc_vin_locale IN ('en_US','es_MX','fr_CA','en_CA');
COMMIT;
UPDATE kapture_dc.gms3_vc_model_year_details SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL AND vc_my_locale IN ('en_US','es_MX','fr_CA','en_CA');
COMMIT;

-- ---------------------------------------------------------------------------------------------
-- 6.4 Display order columns on the MNAO view content tables (codes stay empty, names are filled -
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
-- 6.5 Source path index on gms3_dmt_en_ca_imdoc (en_us / es_mx / fr_ca have it already)
-- ---------------------------------------------------------------------------------------------
CREATE INDEX gms3_dmt_en_ca_imdoc_n2 ON kapture_dc.gms3_dmt_en_ca_imdoc ((TRIM(LOWER(dc_source_network_loc))));
