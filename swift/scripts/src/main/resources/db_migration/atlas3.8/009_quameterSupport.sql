CREATE TABLE quameter_result
(
  quameter_result_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  c_1a               DOUBLE,
  c_1b               DOUBLE,
  c_2a               DOUBLE,
  c_2b               DOUBLE,
  c_3a               DOUBLE,
  c_3b               DOUBLE,
  c_4a               DOUBLE,
  c_4b               DOUBLE,
  c_4c               DOUBLE,
  ds_1a              DOUBLE,
  ds_1b              DOUBLE,
  ds_2a              DOUBLE,
  ds_2b              DOUBLE,
  ds_3a              DOUBLE,
  ds_3b              DOUBLE,
  file_search_id     INT,
  is_1a              DOUBLE,
  is_1b              DOUBLE,
  is_2               DOUBLE,
  is_3a              DOUBLE,
  is_3b              DOUBLE,
  is_3c              DOUBLE,
  ms1_1              DOUBLE,
  ms1_2a             DOUBLE,
  ms1_2b             DOUBLE,
  ms1_3a             DOUBLE,
  ms1_3b             DOUBLE,
  ms1_5a             DOUBLE,
  ms1_5b             DOUBLE,
  ms1_5c             DOUBLE,
  ms1_5d             DOUBLE,
  ms2_1              DOUBLE,
  ms2_2              DOUBLE,
  ms2_3              DOUBLE,
  ms2_4a             DOUBLE,
  ms2_4b             DOUBLE,
  ms2_4c             DOUBLE,
  ms2_4d             DOUBLE,
  p_1                DOUBLE,
  p_2a               DOUBLE,
  p_2b               DOUBLE,
  p_2c               DOUBLE,
  p_3                DOUBLE,
  sample_id          INT,
  KEY quameter_result_ibfk_1 (sample_id),
  KEY quameter_result_ibfk_2 (file_search_id),
  FOREIGN KEY quameter_result_ibfk_1 (sample_id) REFERENCES tandem_mass_spec_sample (tandem_mass_spec_sample_id),
  FOREIGN KEY quameter_result_ibfk_2 (file_search_id) REFERENCES file_search (file_search_id)
);

ALTER TABLE `swift_search_definition`
ADD COLUMN `public_mzxml_files` BIT NULL
AFTER `search_parameters`,
ADD COLUMN `quality_control` BIT NULL
AFTER `public_mzxml_files`;

UPDATE swift_search_definition
SET public_mzxml_files = 0, quality_control = 0
WHERE 1 = 1;

-- @UNDO

ALTER TABLE `swift_search_definition`
DROP COLUMN `quality_control`,
DROP COLUMN `public_mzxml_files`;

DROP TABLE quameter_result;





