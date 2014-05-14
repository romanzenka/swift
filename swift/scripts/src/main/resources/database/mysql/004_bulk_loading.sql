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
DROP TABLE temp_sequence_loading;

SET foreign_key_checks = 1;
