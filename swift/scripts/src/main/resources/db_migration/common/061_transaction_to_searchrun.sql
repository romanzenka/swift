ALTER TABLE `transaction`
CHANGE COLUMN `transaction_id` `search_run_id` INT(11) NOT NULL AUTO_INCREMENT, RENAME TO `search_run`;

ALTER TABLE report CHANGE COLUMN transaction_id search_run_id INT;

ALTER TABLE `task`
DROP FOREIGN KEY `task_ibfk_2`;
ALTER TABLE `task`
CHANGE COLUMN `transaction_id` `search_run_id` INT(11) NULL DEFAULT NULL;
ALTER TABLE `task`
ADD CONSTRAINT `task_ibfk_2`
FOREIGN KEY (`search_run_id`)
REFERENCES `search_run` (`search_run_id`);


-- @UNDO