-- Rename the columns in the tables to match the foreign key (wherever possible)
-- Example: a column with a link to report will be named report_id
-- This way when we see a column ending with _id, we know which table it matches
-- If there are more than two references to the same table, this system breaks down at the moment

ALTER TABLE `file_search`
DROP KEY file_search_ibfk_3,
DROP FOREIGN KEY `file_search_ibfk_3`;

ALTER TABLE `file_search`
CHANGE COLUMN `input_files_id` `swift_search_definition_id` INT(11) NULL DEFAULT NULL;
ALTER TABLE `file_search`
ADD KEY `file_search_ibfk_3` (`swift_search_definition_id`),
ADD CONSTRAINT `file_search_ibfk_3` FOREIGN KEY (`swift_search_definition_id`) REFERENCES `swift_search_definition` (`swift_search_definition_id`);
