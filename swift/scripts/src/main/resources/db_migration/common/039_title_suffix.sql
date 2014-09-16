ALTER TABLE search_parameters
ADD COLUMN `title_suffix` VARCHAR(100) NOT NULL DEFAULT "" AFTER enabled_engines;

-- @UNDO

