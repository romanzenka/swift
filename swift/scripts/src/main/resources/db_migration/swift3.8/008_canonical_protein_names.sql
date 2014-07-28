ALTER TABLE `protein_sequence`
  ADD COLUMN `accession_number` VARCHAR(80) NULL DEFAULT NULL AFTER `sequence`;

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

-- @UNDO

ALTER TABLE biological_sample_list DROP INDEX `hash`;
