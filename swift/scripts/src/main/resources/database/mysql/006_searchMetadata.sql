CREATE TABLE search_metadata
(
  metadata_key               VARCHAR(255) NOT NULL,
  metadata_value             VARCHAR(255),
  swift_search_definition_id INT          NOT NULL,
  PRIMARY KEY (swift_search_definition_id, metadata_key),
  FOREIGN KEY (swift_search_definition_id) REFERENCES swift_search_definition (swift_search_definition_id)
);

-- @UNDO

DROP TABLE search_metadata;

