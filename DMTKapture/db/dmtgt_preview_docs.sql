-- ---------------------------------------------------------------------------------------------
-- DMT (dmtgt) - CONTENT PREVIEW: documents written by each job
-- ---------------------------------------------------------------------------------------------
-- RUN THIS SCRIPT ON kapture_dc BEFORE the dmtgt.war that has the Preview page is deployed.
-- Plain SQL statements only (no procedures, no DELIMITER, no PREPARE), so it runs in any client:
-- SQL Developer (Run Script / F5), Workbench (Execute All) or the mysql client.
-- Every table is written with its schema (kapture_dc.) - change it there if the schema of the
-- target environment has another name. Running it again changes nothing (IF NOT EXISTS).
--
-- One row per document per MC job: the document the job created or updated in Kapture (always
-- unpublished), with what the Publish job needs to publish it (document id, Kapture row id of the
-- written version, version, channel, locale, model type = which DMT tables to update).
-- The Publish job of a job publishes that job's documents, then for each one PUBLISHED:
--   updates DC_IM_DOC_PUBLISHED_STATUS (and the other publish details) in the DMT document table,
--   sets vc_content_status = Published in the view content table,
--   deletes its row here. Rows of documents that FAILED to publish are kept.
-- The same document written by two jobs has a row in each - each job keeps its own preview.
--
-- pv_status
--     ACTIVE     written by the job, not published yet
--     DELETED    unpublished by the delete processing of a LATER job (pv_deleted_schedule_id) -
--                never published again; the Publish job reports it with that job's name
--
-- pv_deleted_schedule_id is added by db/dmtgt_publish.sql (run that script after this one).
--
-- The Preview page itself does NOT read this table: everything it shows is in the job's
-- preview folder (application.properties preview.physical.path), written while the job runs.
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
