CREATE TABLE protein_accnum
(
  protein_accnum_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  accession_number  VARCHAR(80)     NOT NULL
);
CREATE UNIQUE INDEX index3 ON protein_accnum (accession_number);

CREATE TABLE protein_description
(
  protein_description_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  description            VARCHAR(200)    NOT NULL
);
CREATE UNIQUE INDEX desc_unique ON protein_description (description);

CREATE TABLE protein_entry
(
  protein_entry_id       INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  curation_id            INT,
  protein_accnum_id      INT,
  protein_description_id INT,
  protein_sequence_id    INT,
  FOREIGN KEY protein_entry_ibfk_1 (protein_accnum_id) REFERENCES protein_accnum (protein_accnum_id),
  FOREIGN KEY protein_entry_ibfk_2 (protein_sequence_id) REFERENCES protein_sequence (protein_sequence_id),
  FOREIGN KEY protein_entry_ibfk_3 (protein_description_id) REFERENCES protein_description (protein_description_id),
  FOREIGN KEY protein_entry_ibfk_4 (curation_id) REFERENCES curation (curation_id)
);
CREATE UNIQUE INDEX index3 ON protein_entry (curation_id, protein_accnum_id, protein_description_id, protein_sequence_id);

-- Split the protein_database_entry into parts
INSERT INTO protein_accnum (accession_number)
  SELECT
    DISTINCT
    accession_number
  FROM `protein_database_entry`
  ORDER BY accession_number;

INSERT INTO protein_description (description)
  SELECT
    DISTINCT
    description
  FROM `protein_database_entry`
  ORDER BY description;

INSERT INTO protein_entry (protein_entry_id, curation_id, protein_accnum_id, protein_description_id, protein_sequence_id)
  SELECT
    e.protein_database_entry_id,
    e.curation_id,
    a.protein_accnum_id,
    d.protein_description_id,
    e.protein_sequence_id
  FROM protein_database_entry AS e, protein_accnum AS a, protein_description AS d
  WHERE e.accession_number = a.accession_number AND e.description = d.description
  ORDER BY e.protein_database_entry_id;


CREATE PROCEDURE `update_canonical_protein_names`()
-- Define a stored procedure that associates "canonical" accession numbers ot our protein sequences
  UPDATE protein_sequence AS ps
  SET ps.accession_number = (SELECT
                               pa2.accession_number AS newAccnum
                             FROM protein_accnum AS pa2 INNER JOIN
                               protein_entry AS pe2
                                 ON pe2.protein_accnum_id = pa2.protein_accnum_id
                             WHERE pe2.protein_sequence_id = ps.protein_sequence_id
                             ORDER BY pa2.protein_accnum_id
                             LIMIT 1)
  WHERE ps.accession_number IS NULL;

CALL update_canonical_protein_names();

-- Finally drop the obsolete table
DROP TABLE protein_database_entry;

-- @UNDO

-- TODO: We would need to re-create protein_database_entry