ALTER TABLE analysis
DROP KEY analysis_idx_1,
DROP FOREIGN KEY analysis_ibfk_1;

ALTER TABLE analysis
DROP COLUMN report_id,
DROP INDEX uniqueness;

# Search engines no longer evolvable - the record never goes away
ALTER TABLE `search_engine`
DROP KEY `search_engine_idx_2`,
DROP KEY `search_engine_idx_1`,
DROP FOREIGN KEY `search_engine_ibfk_2`,
DROP FOREIGN KEY `search_engine_ibfk_1`;

ALTER TABLE `search_engine`
DROP COLUMN `deletion`,
DROP COLUMN `creation`;

-- @UNDO

# No undo
