ALTER TABLE search_parameters
ADD COLUMN min_termini_cleavages INT
AFTER missed_cleavages;

-- Set everything to fully tryptic (could not do semitryptic searches before)
UPDATE search_parameters
SET min_termini_cleavages = 2
WHERE search_parameter_id > 0;

ALTER TABLE scaffold_settings
ADD COLUMN use_3x_scoring BIT
AFTER high_mass_accuracy_scoring;

UPDATE scaffold_settings
SET mz_ident_ml_report = 0, high_mass_accuracy_scoring = 0, use_3x_scoring = 0
WHERE scaffold_settings_id > 0;


-- @UNDO

