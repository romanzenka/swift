ALTER TABLE quameter_result
ADD COLUMN identified_spectra INT NULL
AFTER `p_3`;

-- @UNDO

ALTER TABLE quameter_result
DROP COLUMN identified_spectra;