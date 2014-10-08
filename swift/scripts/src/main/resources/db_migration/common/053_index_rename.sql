ALTER TABLE protein_accnum DROP INDEX index3;
CREATE UNIQUE INDEX accession_number ON protein_accnum (accession_number);

ALTER TABLE protein_description DROP INDEX desc_unique;
CREATE UNIQUE INDEX description ON protein_description (description);

-- @UNDO

#TBD