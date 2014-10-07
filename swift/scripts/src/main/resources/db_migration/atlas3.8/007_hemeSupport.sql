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
  KEY heme_test_ibfk_1 (search_run),
  FOREIGN KEY heme_test_ibfk_1 (search_run) REFERENCES transaction (transaction_id)
);


-- @UNDO

DROP TABLE heme_test;

