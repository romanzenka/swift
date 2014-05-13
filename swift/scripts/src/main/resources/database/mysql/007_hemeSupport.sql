-- Heme pathology

CREATE TABLE heme_test
(
  date                 DATE            NOT NULL,
  heme_test_id         INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  mass_delta           DOUBLE          NOT NULL,
  mass_delta_tolerance DOUBLE          NOT NULL,
  name                 LONGTEXT        NOT NULL,
  path                 LONGTEXT        NOT NULL,
  search_run           INT,
  FOREIGN KEY (search_run) REFERENCES transaction (transaction_id)
);


-- @UNDO

DROP TABLE heme_test;

