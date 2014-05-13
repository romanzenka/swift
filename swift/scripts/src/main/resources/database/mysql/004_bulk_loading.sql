CREATE TABLE bulk_load_job
(
  bulk_load_job_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  job_date         DATETIME        NOT NULL
);

CREATE TABLE temp_hashed_set
(
  data_order INT    NOT NULL,
  hash       BIGINT NOT NULL,
  job        INT    NOT NULL,
  new_id     INT,
  PRIMARY KEY (job, data_order)
);

CREATE TABLE temp_hashed_set_member
(
  data_order INT NOT NULL,
  job        INT NOT NULL,
  value      INT NOT NULL,
  PRIMARY KEY (job, data_order, value)
);

CREATE TABLE temp_identified_peptide
(
  data_order            INT NOT NULL,
  job                   INT NOT NULL,
  localized_mod_list_id INT NOT NULL,
  new_id                INT,
  peptide_sequence_id   INT NOT NULL,
  PRIMARY KEY (job, data_order),
  FOREIGN KEY (peptide_sequence_id) REFERENCES peptide_sequence (peptide_sequence_id),
  FOREIGN KEY (localized_mod_list_id) REFERENCES localized_mod_list (localized_mod_list_id)
);

CREATE TABLE temp_localized_modification
(
  data_order     INT NOT NULL,
  job            INT NOT NULL,
  new_id         INT,
  position       INT,
  residue        CHAR(1),
  specificity_id INT,
  PRIMARY KEY (job, data_order),
  FOREIGN KEY (specificity_id) REFERENCES mod_specificity (specificity_id)
);

CREATE TABLE temp_peptide_spectrum_match
(
  best_id_probability      DOUBLE  NOT NULL,
  data_order               INT     NOT NULL,
  identified_1h_spectra    INT     NOT NULL,
  identified_2h_spectra    INT     NOT NULL,
  identified_3h_spectra    INT     NOT NULL,
  identified_4h_spectra    INT     NOT NULL,
  identified_peptide_id    INT     NOT NULL,
  job                      INT     NOT NULL,
  new_id                   INT,
  next_aa                  CHAR(1) NOT NULL,
  num_enzymatic_terminii   INT     NOT NULL,
  previous_aa              CHAR(1) NOT NULL,
  total_identified_spectra INT     NOT NULL,
  PRIMARY KEY (job, data_order),
  FOREIGN KEY (identified_peptide_id) REFERENCES identified_peptide (identified_peptide_id)
);

CREATE TABLE temp_sequence_loading
(
  data_order INT NOT NULL,
  job        INT NOT NULL,
  mass       DOUBLE,
  new_id     INT,
  sequence   LONGTEXT,
  PRIMARY KEY (job, data_order)
);

-- @UNDO

SET foreign_key_checks = 0;

DROP TABLE bulk_load_job;
DROP TABLE temp_hashed_set;
DROP TABLE temp_hashed_set_member;
DROP TABLE temp_identified_peptide;
DROP TABLE temp_localized_modification;
DROP TABLE temp_peptide_spectrum_match;
DROP TABLE temp_sequence_loading;

SET foreign_key_checks = 1;
