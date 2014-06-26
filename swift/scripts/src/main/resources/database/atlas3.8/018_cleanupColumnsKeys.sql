ALTER TABLE analysis
DROP FOREIGN KEY analysis_ibfk_1;

ALTER TABLE analysis
DROP COLUMN report_id,
DROP INDEX uniqueness;

# curation_step_list has double foreign keys

ALTER TABLE curation_step_list
DROP FOREIGN KEY curation_step_list_ibfk_3;

# Search engines no longer evolvable - the record never goes away
ALTER TABLE `search_engine`
DROP FOREIGN KEY `search_engine_ibfk_2`,
DROP FOREIGN KEY `search_engine_ibfk_1`;
ALTER TABLE `search_engine`
DROP COLUMN `deletion`,
DROP COLUMN `creation`,
DROP INDEX `creation`,
DROP INDEX `deletion`;

# Duplicate of ibfk_2
ALTER TABLE search_parameters
        DROP FOREIGN KEY search_parameter_ibfk_8;



-- @UNDO

# No undo
