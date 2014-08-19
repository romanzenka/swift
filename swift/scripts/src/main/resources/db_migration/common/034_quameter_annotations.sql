CREATE TABLE `quameter_annotation` (
  `quameter_annotation_id` INT NOT NULL,
  `metric_code` VARCHAR(10) NULL,
  `quameter_result_id` INT NULL,
  `annotation_text` TEXT NULL,
  PRIMARY KEY (`quameter_annotation_id`));

ALTER TABLE `quameter_annotation`
ADD INDEX `quameter_result_ibfk_1_idx` (`quameter_result_id` ASC);
ALTER TABLE `quameter_annotation`
ADD CONSTRAINT `quameter_result_ibfk_1`
  FOREIGN KEY (`quameter_result_id`)
  REFERENCES `quameter_result` (`quameter_result_id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

-- @UNDO

DROP TABLE `quameter_annotation`;