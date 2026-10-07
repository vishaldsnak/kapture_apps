-- ---------------------------------------------------------------------------------------------
-- DMT (dmtgt) - PRE-DEPLOYMENT SCRIPT FOR THE MME MARKET
--   New column vc_content_status on the 14 VIEW CONTENT tables of the MME market
--   (gms3_vc_mme_vin_dtl_<locale>) - the same three steps 1a-1c that dmtgt_pre_deployment.sql
--   ran for the MC table gms3_vc_japan_vin_details.
-- ---------------------------------------------------------------------------------------------
-- RUN THIS SCRIPT ON kapture_dc BEFORE THE dmtgt.war WITH THE MME CHANGES IS DEPLOYED.
-- dmtgt_pre_deployment.sql (MC column + dc_load_type), dmtgt_preview_docs.sql and dmtgt_publish.sql
-- must have been run already - nothing else is needed for MME: the preview and publish tables
-- and dc_load_type are shared by both markets.
--
-- Plain SQL statements only (no procedures, no DELIMITER, no PREPARE), so it runs in any client:
-- SQL Developer (Run Script / F5), Workbench (Execute All) or the mysql client.
-- Every table is written with its schema (kapture_dc.) - change it there if the schema of the
-- target environment has another name.
--
-- RUNNING IT AGAIN: the ADD COLUMN statements then fail with "Duplicate column name" - that only
-- means the column is already there and is harmless; the other statements still run.
--
-- vc_content_status holds the content status of a view content row:
--     Draft      written by DMT when it processes a document
--     Published  set by the Publish Content job
-- The two values are configured in application.properties and MUST be the same as used here:
--     view.content.status.draft / view.content.status.published
--
-- How the existing rows become 'Published':
--   The column is added with DEFAULT 'Published' using ALGORITHM=INSTANT - every existing row
--   reads 'Published' at once, no row is rewritten. The default is then set back to NULL, which
--   only affects rows inserted afterwards. The UPDATE only finds rows when the script is run again.
-- ---------------------------------------------------------------------------------------------

-- 1. kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_cs_cz
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 2. kapture_dc.gms3_vc_mme_vin_dtl_de_de
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_de_de
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_de_de
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_de_de
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 3. kapture_dc.gms3_vc_mme_vin_dtl_el_gr
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_el_gr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_el_gr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_el_gr
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 4. kapture_dc.gms3_vc_mme_vin_dtl_en_uk
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_en_uk
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_en_uk
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_en_uk
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 5. kapture_dc.gms3_vc_mme_vin_dtl_es_es
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_es_es
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_es_es
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_es_es
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 6. kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_fi_fi
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 7. kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_fr_fr
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 8. kapture_dc.gms3_vc_mme_vin_dtl_it_it
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_it_it
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_it_it
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_it_it
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 9. kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_nl_nl
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 10. kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_pl_pl
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 11. kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_pt_pt
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 12. kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_ru_ru
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 13. kapture_dc.gms3_vc_mme_vin_dtl_sv_se
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_sv_se
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_sv_se
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_sv_se
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- 14. kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;
ALTER TABLE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
  ALTER COLUMN vc_content_status SET DEFAULT NULL;
UPDATE kapture_dc.gms3_vc_mme_vin_dtl_tr_tr
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;
COMMIT;

-- ---------------------------------------------------------------------------------------------
-- Check: the new column on all 14 tables (expected: 14 rows, default NULL)
-- ---------------------------------------------------------------------------------------------
SELECT table_name, column_name, column_type, column_default
  FROM information_schema.columns
 WHERE table_schema = 'kapture_dc'
   AND table_name LIKE 'gms3!_vc!_mme!_vin!_dtl!_%' ESCAPE '!'
   AND column_name = 'vc_content_status'
 ORDER BY table_name;
