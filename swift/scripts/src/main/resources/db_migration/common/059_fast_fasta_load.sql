CREATE TABLE temp_string_loading
(
  data_order INT NOT NULL,
  job        INT NOT NULL,
  new_id INT DEFAULT NULL,
  data   VARCHAR(200),
  PRIMARY KEY (job, data_order)
);

-- @UNDO