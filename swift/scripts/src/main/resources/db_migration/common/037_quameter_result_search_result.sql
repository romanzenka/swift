# QuameterResult needs to point to SearchResult instead of TandemMassSpectrometrySample

ALTER TABLE `quameter_result`
ADD COLUMN `search_result_id` INT(11) NULL DEFAULT NULL AFTER `quameter_result_id`,
ADD INDEX `quameter_result_idx_3` (`search_result_id` ASC);
ALTER TABLE `quameter_result`
ADD CONSTRAINT `quameter_result_ibfk_3`
  FOREIGN KEY (`search_result_id`)
  REFERENCES `search_result` (`search_result_id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;

UPDATE quameter_result as q
SET q.search_result_id = (
  select max(s.search_result_id) from search_result as s where s.tandem_mass_spec_sample_id = q.sample_id
  group by s.tandem_mass_spec_sample_id);

ALTER TABLE `quameter_result`
DROP FOREIGN KEY `quameter_result_ibfk_1`;
ALTER TABLE `quameter_result`
DROP COLUMN `sample_id`,
DROP INDEX `quameter_result_idx_1` ;

-- @UNDO

-- To be implemented

