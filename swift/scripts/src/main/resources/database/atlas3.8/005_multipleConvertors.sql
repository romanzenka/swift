ALTER TABLE `extract_msn_settings` ADD COLUMN `command` VARCHAR(50) NULL DEFAULT NULL
AFTER `command_line_switches`;

UPDATE `extract_msn_settings`
SET command="extract_msn"
WHERE 1 = 1;

-- @UNDO

ALTER TABLE `extract_msn_settings` DROP COLUMN `command`;




