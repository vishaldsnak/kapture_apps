-- ---------------------------------------------------------------------------------------------
-- DMT (dmtgt) - PRE-DEPLOYMENT SCRIPT
--   1. New column vc_content_status on the VIEW CONTENT table of the MC market
--   2. New column dc_load_type on the schedule table
-- ---------------------------------------------------------------------------------------------
-- RUN THIS SCRIPT ON kapture_dc BEFORE dmtgt.war IS DEPLOYED.
-- Plain SQL statements only (no procedures, no DELIMITER, no PREPARE), so it runs in any client:
-- SQL Developer (Run Script / F5), Workbench (Execute All) or the mysql client.
-- Every table is written with its schema (kapture_dc.) - change it there if the schema of the
-- target environment has another name.
--
-- RUNNING IT AGAIN: the two ADD COLUMN statements then fail with "Duplicate column name" - that
-- only means the column is already there and is harmless; the other statements still run.
--
-- vc_content_status holds the content status of a view content row:
--     Draft      written by DMT when it processes a document
--     Published  set by the Publish Content job
-- The two values are configured in application.properties and MUST be the same as used here:
--     view.content.status.draft / view.content.status.published
--
-- vc_vin_document_status is NOT touched - it keeps being populated exactly as before.
--
-- How the existing rows become 'Published':
--   The column is added with DEFAULT 'Published' using ALGORITHM=INSTANT - every existing row
--   reads 'Published' at once, no row is rewritten and no large transaction is created. The
--   default is then set back to NULL, which only affects rows inserted afterwards. The UPDATE
--   after it only finds rows when the script is run again (rows that are still NULL).
-- ---------------------------------------------------------------------------------------------

-- ---------------------------------------------------------------------------------------------
-- 1. vc_content_status on the MC view content table (gms3_vc_japan_vin_details)
-- ---------------------------------------------------------------------------------------------

-- 1a. ALTER: add the column - every existing row reads 'Published'
ALTER TABLE kapture_dc.gms3_vc_japan_vin_details
  ADD COLUMN vc_content_status VARCHAR(20) NULL DEFAULT 'Published', ALGORITHM=INSTANT;

-- 1b. rows inserted from now on get NULL unless the application gives a value
ALTER TABLE kapture_dc.gms3_vc_japan_vin_details
  ALTER COLUMN vc_content_status SET DEFAULT NULL;

-- 1c. UPDATE: rows without a status (only when the script is run again)
UPDATE kapture_dc.gms3_vc_japan_vin_details
   SET vc_content_status = 'Published'
 WHERE vc_content_status IS NULL;

COMMIT;

-- THE OTHER MARKETS ARE NOT PART OF THIS RELEASE.
-- Their view content tables (MNAO gms3_vc_vin_details, MME gms3_vc_mme_vin_dtl_<locale>) are
-- handled when the market itself is taken up, with the same three steps 1a-1c.

-- ---------------------------------------------------------------------------------------------
-- 2. dc_load_type on the schedule table
-- ---------------------------------------------------------------------------------------------
-- What a schedule loads: MASTER_DATA_WITH_CONTENT or ONLY_CONTENT (option on the Schedule page).
-- Schedules created before this release keep NULL, which is treated as ONLY_CONTENT.

ALTER TABLE kapture_dc.gms3_dmt_conv_schedule
  ADD COLUMN dc_load_type VARCHAR(30) NULL, ALGORITHM=INSTANT;

-- ---------------------------------------------------------------------------------------------
-- 3. Check: the new columns, and the number of MC view content rows per status
--    Expected: both columns listed with default NULL; every existing row 'Published'.
-- ---------------------------------------------------------------------------------------------
SELECT table_name, column_name, column_type, column_default
  FROM information_schema.columns
 WHERE table_schema = 'kapture_dc'
   AND ((table_name = 'gms3_vc_japan_vin_details' AND column_name = 'vc_content_status')
     OR (table_name = 'gms3_dmt_conv_schedule' AND column_name = 'dc_load_type'))
 ORDER BY table_name;

SELECT vc_content_status, COUNT(*) AS row_count
  FROM kapture_dc.gms3_vc_japan_vin_details
 GROUP BY vc_content_status;
