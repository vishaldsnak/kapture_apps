-- ---------------------------------------------------------------------------------------------
-- DMT (dmtgt) - PUBLISH CONTENT JOB
-- ---------------------------------------------------------------------------------------------
-- RUN THIS SCRIPT ON kapture_dc BEFORE the dmtgt.war that has the Publish Content job is deployed,
-- AFTER db/dmtgt_preview_docs.sql. Plain SQL statements only (no procedures, no DELIMITER, no
-- PREPARE), so it runs in any client: SQL Developer (Run Script / F5), Workbench or mysql.
-- Every table is written with its schema (kapture_dc.) - change it there if the schema of the
-- target environment has another name.
--
-- 1. gms3_dmt_preview_docs.pv_deleted_schedule_id - the job whose delete processing unpublished
--    the document (the Publish job names it in its report). ALGORITHM=INSTANT: no table rewrite.
--    RUN ONCE: a second run fails with "Duplicate column name" - that error means it is done.
-- 2. Rows marked WITHDRAWN by an earlier war are renamed DELETED (the job that deleted them was
--    not recorded then, so pv_deleted_schedule_id stays empty for them). Safe to run again.
-- ---------------------------------------------------------------------------------------------

ALTER TABLE kapture_dc.gms3_dmt_preview_docs ADD COLUMN pv_deleted_schedule_id BIGINT NULL, ALGORITHM=INSTANT;

UPDATE kapture_dc.gms3_dmt_preview_docs SET pv_status = 'DELETED' WHERE pv_status = 'WITHDRAWN';

COMMIT;
