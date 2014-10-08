CREATE TABLE `task_state_tmp` (
  `task_state_id` INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `description` varchar(60)
);

INSERT INTO `task_state_tmp`(`task_state_id`, `description`)
        SELECT task_state_id, description FROM `task_state`;


ALTER TABLE `task` DROP FOREIGN KEY `task_ibfk_1`;

DROP TABLE task_state;

CREATE TABLE `task_state` (
  `task_state_id` INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `description` varchar(60)
);

INSERT INTO `task_state`(`task_state_id`, `description`)
        SELECT task_state_id, description FROM `task_state_tmp`;

DROP TABLE task_state_tmp;

ALTER TABLE `task`
  CHANGE COLUMN `task_state` `task_state` INT NULL DEFAULT NULL ;

ALTER TABLE `task`
  ADD FOREIGN KEY task_ibfk_1 (task_state) REFERENCES task_state (task_state_id);


-- @UNDO

#TBD
