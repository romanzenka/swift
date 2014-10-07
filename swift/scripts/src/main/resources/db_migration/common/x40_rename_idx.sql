# select concat("ALTER TABLE `", table_name, "` DROP INDEX `", index_name, "`, ADD INDEX `", replace(index_name, "_idx_", "_ibfk_"), "` (`", column_name, "`);") as renames from information_schema.statistics where table_schema="atlas_migration" and index_name like "%_idx_%";

ALTER TABLE `analysis` DROP INDEX `analysis_idx_2`, ADD INDEX `analysis_ibfk_2` (`biological_sample_list_id`);
ALTER TABLE `biological_sample` DROP INDEX `biological_sample_idx_1`, ADD INDEX `biological_sample_ibfk_1` (`search_result_list_id`);
ALTER TABLE `biological_sample_list_members` DROP INDEX `biological_sample_list_members_idx_1`, ADD INDEX `biological_sample_list_members_ibfk_1` (`biological_sample_id`);
ALTER TABLE `biological_sample_list_members` DROP INDEX `biological_sample_list_members_idx_2`, ADD INDEX `biological_sample_list_members_ibfk_2` (`biological_sample_list_id`);
ALTER TABLE `curation` DROP INDEX `curation_idx_1`, ADD INDEX `curation_ibfk_1` (`deletion`);
ALTER TABLE `curation` DROP INDEX `curation_idx_2`, ADD INDEX `curation_ibfk_2` (`creation`);
ALTER TABLE `curation_data_source` DROP INDEX `curation_idx_1`, ADD INDEX `curation_data_source_ibfk_1` (`auto_transform`);
ALTER TABLE `curation_step_database_upload` DROP INDEX `curation_step_database_upload_idx_1`, ADD INDEX `curation_step_database_upload_ibfk_1` (`upload_id`);
ALTER TABLE `curation_step_header_filter` DROP INDEX `curation_step_header_filter_idx_1`, ADD INDEX `curation_step_header_filter_ibfk_1` (`header_filter_id`);
ALTER TABLE `curation_step_header_transform` DROP INDEX `curation_step_header_transform_idx_1`, ADD INDEX `curation_step_header_transform_ibfk_1` (`header_transform_id`);
ALTER TABLE `curation_step_list` DROP INDEX `curation_step_list_idx_1`, ADD INDEX `curation_step_list_ibfk_1` (`step_id`);
ALTER TABLE `curation_step_list` DROP INDEX `curation_step_list_idx_2`, ADD INDEX `curation_step_list_ibfk_2` (`curation_id`);
ALTER TABLE `curation_step_make_decoy` DROP INDEX `curation_step_make_decoy_idx_1`, ADD INDEX `curation_step_make_decoy_ibfk_1` (`sequence_manipulation_id`);
ALTER TABLE `curation_step_manual_inclusion` DROP INDEX `curation_step_manual_inclusion_idx_1`, ADD INDEX `curation_step_manual_inclusion_ibfk_1` (`manual_inclusion_id`);
ALTER TABLE `curation_step_new_db` DROP INDEX `curation_step_new_db_idx_1`, ADD INDEX `curation_step_new_db_ibfk_1` (`source_db`);
ALTER TABLE `curation_step_new_db` DROP INDEX `curation_step_new_db_idx_2`, ADD INDEX `curation_step_new_db_ibfk_2` (`new_database_id`);
ALTER TABLE `enabled_engines_set` DROP INDEX `enabled_engines_set_idx_1`, ADD INDEX `enabled_engines_set_ibfk_1` (`set_id`);
ALTER TABLE `enabled_engines_set` DROP INDEX `enabled_engines_set_idx_2`, ADD INDEX `enabled_engines_set_ibfk_2` (`search_engine_id`);
ALTER TABLE `file_search`
  DROP INDEX `file_search_idx_1`,
  DROP INDEX `file_search_idx_3`,
  ADD INDEX `file_search_ibfk_1` (`search_parameters`),
  ADD INDEX `file_search_ibfk_3` (`swift_search_definition_id`);
ALTER TABLE `heme_test` DROP INDEX `heme_test_idx_1`, ADD INDEX `heme_test_ibfk_1` (`search_run`);
ALTER TABLE `instrument`
  DROP INDEX `instrument_idx_1`,
  DROP INDEX `instrument_idx_2`,
  ADD INDEX `instrument_ibfk_1` (`deletion`),
  ADD INDEX `instrument_ibfk_2` (`creation`);
ALTER TABLE `instrument_series`
  DROP INDEX `instrument_series_idx_1`,
  DROP INDEX `instrument_series_idx_2`,
  ADD INDEX `instrument_series_ibfk_1` (`ion_series_id`),
  ADD INDEX `instrument_series_ibfk_2` (`instrument_id`);
ALTER TABLE `ion_series`
  DROP INDEX `ion_series_idx_1`,
  DROP INDEX `ion_series_idx_2`,
  ADD INDEX `ion_series_ibfk_1` (`deletion`),
  ADD INDEX `ion_series_ibfk_2` (`creation`);
ALTER TABLE `log` DROP INDEX `log_data_idx_2`, ADD INDEX `log_data_ibfk_2` (`parent_log_id`);
ALTER TABLE `log` DROP INDEX `log_data_idx_1`, ADD INDEX `log_data_ibfk_1` (`task_id`);
ALTER TABLE `mod` DROP INDEX `mod_idx_1`, ADD INDEX `mod_ibfk_1` (`deletion`);
ALTER TABLE `mod` DROP INDEX `mod_idx_2`, ADD INDEX `mod_ibfk_2` (`creation`);
ALTER TABLE `mod_alt_names` DROP INDEX `mod_alt_names_idx_1`, ADD INDEX `mod_alt_names_ibfk_1` (`alt_name_id`);
ALTER TABLE `mod_set_specificities`
  DROP INDEX `mod_set_specificities_idx_1`,
  DROP INDEX `mod_set_specificities_idx_2`,
  ADD INDEX `mod_set_specificities_ibfk_1` (`specificity_id`),
  ADD INDEX `mod_set_specificities_ibfk_2` (`set_id`);
ALTER TABLE `mod_specificity` DROP INDEX `mod_specificity_idx_1`, ADD INDEX `mod_specificity_ibfk_1` (`mod_id`);
ALTER TABLE `protease`
  DROP INDEX `protease_idx_1`,
  DROP INDEX `protease_idx_2`,
  ADD INDEX `protease_ibfk_1` (`deletion`),
  ADD INDEX `protease_ibfk_2` (`creation`);
ALTER TABLE `protein_entry`
  DROP INDEX `protein_entry_idx_1`,
  DROP INDEX `protein_entry_idx_2`,
  DROP INDEX `protein_entry_idx_3`,
  DROP INDEX `protein_entry_idx_4`,
  ADD INDEX `protein_entry_ibfk_1` (`protein_accnum_id`),
  ADD INDEX `protein_entry_ibfk_2` (`protein_sequence_id`),
  ADD INDEX `protein_entry_ibfk_3` (`protein_description_id`),
  ADD INDEX `protein_entry_ibfk_4` (`curation_id`);
ALTER TABLE `protein_group` DROP INDEX `protein_group_idx_1`, ADD INDEX `protein_group_ibfk_1` (`protein_sequence_list_id`);
ALTER TABLE `protein_group_list_members`
  DROP INDEX `protein_group_list_members_idx_1`,
  DROP INDEX `protein_group_list_members_idx_2`,
  ADD INDEX `protein_group_list_members_ibfk_1` (`protein_group_id`),
  ADD INDEX `protein_group_list_members_ibfk_2` (`protein_group_list_id`);
ALTER TABLE `protein_sequence_list_members` DROP INDEX `protein_sequence_list_members_idx_1`, DROP INDEX `protein_sequence_list_members_idx_2`, ADD INDEX `protein_sequence_list_members_ibfk_1` (`protein_sequence_id`), ADD INDEX `protein_sequence_list_members_ibfk_2` (`protein_sequence_list_id`);
ALTER TABLE `quameter_annotation` DROP INDEX `quameter_annotation_idx_1`, ADD INDEX `quameter_annotation_ibfk_1` (`quameter_result_id`);
ALTER TABLE `quameter_pg` DROP INDEX `quameter_pg_idx_1`, DROP INDEX `quameter_pg_idx_2`, ADD INDEX `quameter_pg_ibfk_1` (`deletion`), ADD INDEX `quameter_pg_ibfk_2` (`creation`);
ALTER TABLE `quameter_result`
  DROP INDEX `quameter_result_idx_2`,
  DROP INDEX `quameter_result_idx_3`,
  ADD INDEX `quameter_result_ibfk_2` (`file_search_id`),
  ADD INDEX `quameter_result_ibfk_3` (`search_result_id`);
ALTER TABLE `quameter_spectra`
  DROP INDEX `quameter_spectra_idx_1`,
  DROP INDEX `quameter_spectra_idx_2`,
  ADD INDEX `quameter_spectra_ibfk_1` (`quameter_result_id`),
  ADD INDEX `quameter_spectra_ibfk_2` (`quameter_pg_id`);
ALTER TABLE `report` DROP INDEX `report_idx_1`, ADD INDEX `report_ibfk_1` (`transaction_id`);
ALTER TABLE `report` DROP INDEX `report_idx_2`, ADD INDEX `report_ibfk_2` (`analysis_id`);
ALTER TABLE `saved_parameters`
  DROP INDEX `saved_parameters_idx_1`,
  DROP INDEX `saved_parameters_idx_2`,
  DROP INDEX `saved_parameters_idx_3`,
  DROP INDEX `saved_parameters_idx_4`,
  ADD INDEX `saved_parameters_ibfk_1` (`owner`),
  ADD INDEX `saved_parameters_ibfk_2` (`deletion`),
  ADD INDEX `saved_parameters_ibfk_3` (`creation`),
  ADD INDEX `saved_parameters_ibfk_4` (`parameters`);
ALTER TABLE `scaffold_settings` DROP INDEX `scaffold_settings_idx_1`, ADD INDEX `scaffold_settings_ibfk_1` (`starred_proteins`);
ALTER TABLE `search_metadata` DROP INDEX `search_metadata_idx_1`, ADD INDEX `search_metadata_ibfk_1` (`swift_search_definition_id`);
ALTER TABLE `search_parameters`
  DROP INDEX `search_parameters_idx_1`,
  DROP INDEX `search_parameters_idx_2`,
  DROP INDEX `search_parameters_idx_3`,
  DROP INDEX `search_parameters_idx_4`,
  DROP INDEX `search_parameters_idx_5`,
  DROP INDEX `search_parameters_idx_6`,
  DROP INDEX `search_parameters_idx_7`,
  DROP INDEX `search_parameters_idx_8`,
  DROP INDEX `search_parameters_idx_9`,
  ADD INDEX `search_parameters_ibfk_1` (`variable_mods`),
  ADD INDEX `search_parameters_ibfk_2` (`scaffold_settings`),
  ADD INDEX `search_parameters_ibfk_3` (`fixed_mods`),
  ADD INDEX `search_parameters_ibfk_4` (`protease`),
  ADD INDEX `search_parameters_ibfk_5` (`instrument`),
  ADD INDEX `search_parameters_ibfk_6` (`curation`),
  ADD INDEX `search_parameters_ibfk_7` (`extract_msn_settings`),
  ADD INDEX `search_parameters_ibfk_8` (`scaffold_settings`),
  ADD INDEX `search_parameters_ibfk_9` (`enabled_engines`);
ALTER TABLE `search_result`
  DROP INDEX `search_result_idx_1`,
  DROP INDEX `search_result_idx_2`,
  ADD INDEX `search_result_ibfk_1` (`protein_group_list_id`),
  ADD INDEX `search_result_ibfk_2` (`tandem_mass_spec_sample_id`);
ALTER TABLE `search_result_list_members`
  DROP INDEX `search_result_list_members_idx_1`,
  DROP INDEX `search_result_list_members_idx_2`,
  ADD INDEX `search_result_list_members_ibfk_1` (`search_result_list_id`),
  ADD INDEX `search_result_list_members_ibfk_2` (`search_result_id`);
ALTER TABLE `swift_search_definition`
  DROP INDEX `swift_search_definition_idx_1`,
  DROP INDEX `swift_search_definition_idx_2`,
  DROP INDEX `swift_search_definition_idx_3`,
  DROP INDEX `swift_search_definition_idx_4`,
  ADD INDEX `swift_search_definition_ibfk_1` (`peptide_report`),
  ADD INDEX `swift_search_definition_ibfk_2` (`search_parameters`),
  ADD INDEX `swift_search_definition_ibfk_3` (`spectrum_qa`),
  ADD INDEX `swift_search_definition_ibfk_4` (`owner`);
ALTER TABLE `task`
  DROP INDEX `task_idx_1`,
  DROP INDEX `task_idx_2`,
  ADD INDEX `task_ibfk_1` (`task_state`),
  ADD INDEX `task_ibfk_2` (`transaction_id`);
ALTER TABLE `transaction`
  DROP INDEX `transaction_idx_1`,
  DROP INDEX `transaction_idx_2`,
  ADD INDEX `transaction_ibfk_1` (`submitting_user`),
  ADD INDEX `transaction_ibfk_2` (`swift_search`);
ALTER TABLE `user_preferences`
  DROP INDEX `user_preferences_idx_1`,
  ADD INDEX `user_preferences_ibfk_1` (`user_id`);
ALTER TABLE `workflow_user`
  DROP INDEX `workflow_user_idx_1`,
  DROP INDEX `workflow_user_idx_2`,
  ADD INDEX `workflow_user_ibfk_1` (`deletion`),
  ADD INDEX `workflow_user_ibfk_2` (`creation`);

-- @UNDO
