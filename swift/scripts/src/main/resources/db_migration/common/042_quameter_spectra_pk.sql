# map primary keys

ALTER TABLE `quameter_spectra`
DROP FOREIGN KEY `quameter_spectra_ibfk_1`,
DROP FOREIGN KEY `quameter_spectra_ibfk_2`;

ALTER TABLE `quameter_spectra`
DROP COLUMN `quameter_spectra_id`,
CHANGE COLUMN `quameter_pg_id` `quameter_pg_id` INT(11) NOT NULL FIRST,
CHANGE COLUMN `quameter_result_id` `quameter_result_id` INT(11) NOT NULL,
DROP PRIMARY KEY,
ADD PRIMARY KEY (`quameter_result_id`, `quameter_pg_id`);

ALTER TABLE `quameter_spectra`
ADD CONSTRAINT `quameter_spectra_ibfk_1`
  FOREIGN KEY (`quameter_result_id`)
  REFERENCES `quameter_result` (`quameter_result_id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT `quameter_spectra_ibfk_2`
  FOREIGN KEY (`quameter_pg_id`)
  REFERENCES `quameter_pg` (`quameter_pg_id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

-- @UNDO

# TBD