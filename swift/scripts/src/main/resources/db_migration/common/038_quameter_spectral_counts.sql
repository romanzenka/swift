CREATE TABLE `quameter_spectra` (
  `quameter_spectra_id` INT NOT NULL AUTO_INCREMENT,
  `quameter_result_id` INT NULL,
  `quameter_pg_id` INT NULL,
  `unique_spectra` INT NULL,
  PRIMARY KEY (`quameter_spectra_id`),
  INDEX `quameter_spectra_idx_1` (`quameter_result_id` ASC),
  INDEX `quameter_spectra_idx_2` (`quameter_pg_id` ASC),
  CONSTRAINT `quameter_spectra_ibfk_1`
    FOREIGN KEY (`quameter_result_id`)
    REFERENCES `quameter_result` (`quameter_result_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `quameter_spectra_ibfk_2`
    FOREIGN KEY (`quameter_pg_id`)
    REFERENCES `quameter_pg` (`quameter_pg_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

-- @UNDO