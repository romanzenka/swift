ALTER TABLE `atlas_migration`.`curation_step_new_db`
DROP FOREIGN KEY `curation_step_new_db_ibfk_1`;
ALTER TABLE `atlas_migration`.`curation_step_new_db`
DROP COLUMN `source_db`,
DROP INDEX `curation_step_new_db_ibfk_1` ;

-- @UNDO