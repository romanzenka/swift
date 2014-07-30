ALTER TABLE quameter_result
ADD COLUMN hidden BIT DEFAULT 0
AFTER identified_spectra;

-- @UNDO

ALTER TABLE quameter_result
DROP COLUMN hidden;