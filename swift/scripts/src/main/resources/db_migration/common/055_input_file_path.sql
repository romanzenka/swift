ALTER TABLE file_search MODIFY COLUMN input_file VARCHAR(500);
ALTER TABLE tandem_mass_spec_sample MODIFY COLUMN sample_file VARCHAR(500);
ALTER TABLE swift_search_definition MODIFY COLUMN output_folder VARCHAR(500);
ALTER TABLE log MODIFY COLUMN out_log VARCHAR(500), MODIFY COLUMN err_log VARCHAR(500);
ALTER TABLE source_database_archive MODIFY COLUMN source_path VARCHAR(500);
ALTER TABLE curation MODIFY COLUMN curated_filename VARCHAR(500);

-- @UNDO