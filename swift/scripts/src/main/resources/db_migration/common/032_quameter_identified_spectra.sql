ALTER TABLE quameter_result
ADD COLUMN identified_spectra INT NULL
AFTER `p_3`;

UPDATE quameter_result
SET identified_spectra=0
WHERE identified_spectra IS NULL;

-- @UNDO

ALTER TABLE quameter_result
DROP COLUMN identified_spectra;