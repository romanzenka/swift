ALTER TABLE `swift`.`transaction`
CHANGE COLUMN `transaction_id` `search_run_id` INT(11) NOT NULL AUTO_INCREMENT, RENAME TO `swift`.`search_run`;

-- @UNDO