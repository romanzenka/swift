SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE identified_peptide;

DROP TABLE localized_mod_list;
DROP TABLE localized_mod_list_members;
DROP TABLE localized_modification;

DROP TABLE peptide_sequence;
DROP TABLE peptide_spectrum_match;
DROP TABLE peptide_spectrum_match_list;
DROP TABLE peptide_spectrum_match_list_members;

DROP TABLE temp_identified_peptide;
DROP TABLE temp_localized_modification;
DROP TABLE temp_peptide_spectrum_match;

SET FOREIGN_KEY_CHECKS = 1;

-- @UNDO

# Cannot be undone