ALTER TABLE analysis
  DROP FOREIGN KEY `FKC2F032DC2FCBA4B`,
  DROP FOREIGN KEY `FKC2F032DCE5C0C904`;
ALTER TABLE analysis ADD FOREIGN KEY analysis_ibfk_1 (report_id) REFERENCES report (report_id);
ALTER TABLE analysis ADD FOREIGN KEY analysis_ibfk_2 (biological_sample_list_id) REFERENCES biological_sample_list (biological_sample_list_id);


ALTER TABLE biological_sample
  DROP FOREIGN KEY `FK1F30F9A831BFFCCA`;
ALTER TABLE biological_sample  ADD FOREIGN KEY biological_sample_ibfk_1 (search_result_list_id) REFERENCES search_result_list (search_result_list_id);


ALTER TABLE biological_sample_list_members
  DROP FOREIGN KEY `FK85F503AFAD23B713`,
  DROP FOREIGN KEY `FK85F503AFE5C0C904`;
ALTER TABLE biological_sample_list_members ADD FOREIGN KEY biological_sample_list_members_ibfk_1 (biological_sample_id) REFERENCES biological_sample (biological_sample_id);
ALTER TABLE biological_sample_list_members ADD FOREIGN KEY biological_sample_list_members_ibfk_2 (biological_sample_list_id) REFERENCES biological_sample_list (biological_sample_list_id);

ALTER TABLE curation
  DROP FOREIGN KEY `FK216320B52D806023`,
  DROP FOREIGN KEY `FK216320B56925EB34`;
ALTER TABLE curation ADD FOREIGN KEY curation_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE curation ADD FOREIGN KEY curation_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);


ALTER TABLE curation_data_source
  DROP FOREIGN KEY `FKB3C179C692A49BF2`;
ALTER TABLE curation_data_source ADD FOREIGN KEY curation_ibfk_1 (auto_transform) REFERENCES curation_header_transform (header_transform_id);

ALTER TABLE curation_step_database_upload DROP FOREIGN KEY `FK38DF9B1C925A14E3`; #curation_step-step_id
ALTER TABLE curation_step_database_upload ADD FOREIGN KEY curation_step_database_upload_ibfk_1 (upload_id) REFERENCES curation_step (step_id);

ALTER TABLE curation_step_header_filter DROP FOREIGN KEY `FK4EF11901E93020DA`; #curation_step-step_id
ALTER TABLE curation_step_header_filter ADD FOREIGN KEY curation_step_header_filter_ibfk_1 (header_filter_id) REFERENCES curation_step (step_id);

ALTER TABLE curation_step_header_transform DROP FOREIGN KEY `FKFE1545432CE9100A`; #curation_step-step_id
ALTER TABLE curation_step_header_transform ADD FOREIGN KEY curation_step_header_transform_ibfk_1 (header_transform_id) REFERENCES curation_step (step_id);

ALTER TABLE curation_step_list DROP FOREIGN KEY `FKD5163F27B74BB9C3`; #curation-curation_id
ALTER TABLE curation_step_list DROP FOREIGN KEY `FKD5163F27D7A16438`; #curation_step-step_id
ALTER TABLE curation_step_list ADD FOREIGN KEY curation_step_list_ibfk_1 (step_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_list ADD FOREIGN KEY curation_step_list_ibfk_2 (curation_id) REFERENCES curation (curation_id);

ALTER TABLE curation_step_make_decoy DROP FOREIGN KEY `FK85AA1BA414917D49`; #curation_step-step_id
ALTER TABLE curation_step_make_decoy ADD FOREIGN KEY curation_step_make_decoy_ibfk_1 (sequence_manipulation_id) REFERENCES curation_step (step_id);

ALTER TABLE curation_step_manual_inclusion DROP FOREIGN KEY `FK196424ECC86A3C1`; #curation_step-step_id
ALTER TABLE curation_step_manual_inclusion ADD FOREIGN KEY curation_step_manual_inclusion_ibfk_1 (manual_inclusion_id) REFERENCES curation_step (step_id);

ALTER TABLE curation_step_new_db DROP FOREIGN KEY `FKEBB5F3C6590926C5`; #source_database_archive-source_id
ALTER TABLE curation_step_new_db DROP FOREIGN KEY `FKEBB5F3C6EBC63F0A`; #curation_step-step_id
ALTER TABLE curation_step_new_db ADD FOREIGN KEY curation_step_new_db_ibfk_1 (source_db) REFERENCES source_database_archive (source_id);
ALTER TABLE curation_step_new_db ADD FOREIGN KEY curation_step_new_db_ibfk_2 (new_database_id) REFERENCES curation_step (step_id);

ALTER TABLE enabled_engines_set DROP FOREIGN KEY `FKF6BDA996151541CF`; #enabled_engines-enabled_engines_id
ALTER TABLE enabled_engines_set DROP FOREIGN KEY `FKF6BDA99674F3894`; #search_engine-search_engine_id
ALTER TABLE enabled_engines_set ADD FOREIGN KEY enabled_engines_set_ibfk_1 (set_id) REFERENCES enabled_engines (enabled_engines_id);
ALTER TABLE enabled_engines_set ADD FOREIGN KEY enabled_engines_set_ibfk_2 (search_engine_id) REFERENCES search_engine (search_engine_id);

ALTER TABLE file_search DROP FOREIGN KEY `FK795E89CB66BE2F2A`; #enabled_engines-enabled_engines_id
ALTER TABLE file_search DROP FOREIGN KEY `FK795E89CBB526F05F`; #swift_search_definition-swift_search_definition_id
ALTER TABLE file_search DROP FOREIGN KEY `fs_sp_fk`; #search_parameters-search_parameter_id
ALTER TABLE file_search ADD FOREIGN KEY file_search_ibfk_1 (search_parameters) REFERENCES search_parameters (search_parameter_id);
ALTER TABLE file_search ADD FOREIGN KEY file_search_ibfk_2 (enabled_engines) REFERENCES enabled_engines (enabled_engines_id);
ALTER TABLE file_search ADD FOREIGN KEY file_search_ibfk_3 (input_files_id) REFERENCES swift_search_definition (swift_search_definition_id);

ALTER TABLE heme_test DROP FOREIGN KEY `FK6F757B3C97903710`; #transaction-transaction_id
ALTER TABLE heme_test ADD FOREIGN KEY heme_test_ibfk_1 (search_run) REFERENCES transaction (transaction_id);

ALTER TABLE instrument DROP FOREIGN KEY `FK532D63E72D806023`; #change_audit-change_audit_id
ALTER TABLE instrument DROP FOREIGN KEY `FK532D63E76925EB34`; #change_audit-change_audit_id
ALTER TABLE instrument ADD FOREIGN KEY instrument_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE instrument ADD FOREIGN KEY instrument_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);

ALTER TABLE instrument_series DROP FOREIGN KEY `FK6C2EB5CF3BF3EC80`; #ion_series-ion_series_id
ALTER TABLE instrument_series DROP FOREIGN KEY `FK6C2EB5CF8B4A6785`; #instrument-instrument_id
ALTER TABLE instrument_series ADD FOREIGN KEY instrument_series_ibfk_1 (ion_series_id) REFERENCES ion_series (ion_series_id);
ALTER TABLE instrument_series ADD FOREIGN KEY instrument_series_ibfk_2 (instrument_id) REFERENCES instrument (instrument_id);

ALTER TABLE ion_series DROP FOREIGN KEY `FK89FECA8E2D806023`; #change_audit-change_audit_id
ALTER TABLE ion_series DROP FOREIGN KEY `FK89FECA8E6925EB34`; #change_audit-change_audit_id
ALTER TABLE ion_series ADD FOREIGN KEY ion_series_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE ion_series ADD FOREIGN KEY ion_series_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);


ALTER TABLE `mod` DROP FOREIGN KEY `FK1A7022D806023`; #change_audit-change_audit_id
ALTER TABLE `mod` DROP FOREIGN KEY `FK1A7026925EB34`; #change_audit-change_audit_id
ALTER TABLE `mod` ADD FOREIGN KEY mod_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE `mod` ADD FOREIGN KEY mod_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);

ALTER TABLE mod_alt_names DROP FOREIGN KEY `FK55FF5835FEACE9CD`; #mod-mod_id
ALTER TABLE mod_alt_names ADD FOREIGN KEY mod_alt_names_ibfk_1 (alt_name_id) REFERENCES `mod` (mod_id);

ALTER TABLE mod_set_specificities DROP FOREIGN KEY `FK26EDCAC02E879086`; #mod_specificity-specificity_id
ALTER TABLE mod_set_specificities DROP FOREIGN KEY `FK26EDCAC0CD2EA2C6`; #mod_set-mod_set_id
ALTER TABLE mod_set_specificities ADD FOREIGN KEY mod_set_specificities_ibfk_1 (specificity_id) REFERENCES mod_specificity (specificity_id);
ALTER TABLE mod_set_specificities ADD FOREIGN KEY mod_set_specificities_ibfk_2 (set_id) REFERENCES mod_set (mod_set_id);

ALTER TABLE mod_specificity DROP FOREIGN KEY `FKC165C7F231FEE8C`; #mod-mod_id
ALTER TABLE mod_specificity ADD FOREIGN KEY mod_specificity_ibfk_1 (mod_id) REFERENCES `mod` (mod_id);

ALTER TABLE protease DROP FOREIGN KEY `FKC505FBD52D806023`; #change_audit-change_audit_id
ALTER TABLE protease DROP FOREIGN KEY `FKC505FBD56925EB34`; #change_audit-change_audit_id
ALTER TABLE protease ADD FOREIGN KEY protease_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE protease ADD FOREIGN KEY protease_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);

ALTER TABLE protein_entry DROP FOREIGN KEY `FK840754B674105268`; #protein_accnum-protein_accnum_id
ALTER TABLE protein_entry DROP FOREIGN KEY `FK840754B6883BA968`; #protein_sequence-protein_sequence_id
ALTER TABLE protein_entry DROP FOREIGN KEY `FK840754B6AF28DA2C`; #protein_description-protein_description_id
ALTER TABLE protein_entry DROP FOREIGN KEY `FK840754B6B74BB9C3`; #curation-curation_id
ALTER TABLE protein_entry ADD FOREIGN KEY protein_entry_ibfk_1 (protein_accnum_id) REFERENCES protein_accnum (protein_accnum_id);
ALTER TABLE protein_entry ADD FOREIGN KEY protein_entry_ibfk_2 (protein_sequence_id) REFERENCES protein_sequence (protein_sequence_id);
ALTER TABLE protein_entry ADD FOREIGN KEY protein_entry_ibfk_3 (protein_description_id) REFERENCES protein_description (protein_description_id);
ALTER TABLE protein_entry ADD FOREIGN KEY protein_entry_ibfk_4 (curation_id) REFERENCES curation (curation_id);


ALTER TABLE protein_group DROP FOREIGN KEY `FK842542C3D7502D86`; #protein_sequence_list-protein_sequence_list_id
ALTER TABLE protein_group ADD FOREIGN KEY protein_group_ibfk_1 (protein_sequence_list_id) REFERENCES protein_sequence_list (protein_sequence_list_id);

ALTER TABLE protein_group_list_members DROP FOREIGN KEY `FKDE639E74856764C9`; #protein_group-protein_group_id
ALTER TABLE protein_group_list_members DROP FOREIGN KEY `FKDE639E74C1265750`; #protein_group_list-protein_group_list_id
ALTER TABLE protein_group_list_members ADD FOREIGN KEY protein_group_list_members_ibfk_1 (protein_group_id) REFERENCES protein_group (protein_group_id);
ALTER TABLE protein_group_list_members ADD FOREIGN KEY protein_group_list_members_ibfk_2 (protein_group_list_id) REFERENCES protein_group_list (protein_group_list_id);

ALTER TABLE protein_sequence_list_members DROP FOREIGN KEY `FK1C463A7A883BA968`; #protein_sequence-protein_sequence_id
ALTER TABLE protein_sequence_list_members DROP FOREIGN KEY `FK1C463A7AD7502D86`; #protein_sequence_list-protein_sequence_list_id
ALTER TABLE protein_sequence_list_members ADD FOREIGN KEY protein_sequence_list_members_ibfk_1 (protein_sequence_id) REFERENCES protein_sequence (protein_sequence_id);
ALTER TABLE protein_sequence_list_members ADD FOREIGN KEY protein_sequence_list_members_ibfk_2 (protein_sequence_list_id) REFERENCES protein_sequence_list (protein_sequence_list_id);

ALTER TABLE report DROP FOREIGN KEY `FKC84C5534FE2045B8`; #transaction-transaction_id
ALTER TABLE report ADD FOREIGN KEY report_ibfk_1 (transaction_id) REFERENCES transaction (transaction_id);

ALTER TABLE saved_parameters DROP FOREIGN KEY `FKF4311EA2244FDD65`; #workflow_user-workflow_user_id
ALTER TABLE saved_parameters DROP FOREIGN KEY `FKF4311EA22D806023`; #change_audit-change_audit_id
ALTER TABLE saved_parameters DROP FOREIGN KEY `FKF4311EA26925EB34`; #change_audit-change_audit_id
ALTER TABLE saved_parameters DROP FOREIGN KEY `FKF4311EA2AB09B429`; #search_parameters-search_parameter_id
ALTER TABLE saved_parameters ADD FOREIGN KEY saved_parameters_ibfk_1 (owner) REFERENCES workflow_user (workflow_user_id);
ALTER TABLE saved_parameters ADD FOREIGN KEY saved_parameters_ibfk_2 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE saved_parameters ADD FOREIGN KEY saved_parameters_ibfk_3 (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE saved_parameters ADD FOREIGN KEY saved_parameters_ibfk_4 (parameters) REFERENCES search_parameters (search_parameter_id);

ALTER TABLE scaffold_settings DROP FOREIGN KEY `FK7887F24C9CB277B4`; #starred_proteins-starred_proteins_id
ALTER TABLE scaffold_settings ADD FOREIGN KEY scaffold_settings_ibfk_1 (starred_proteins) REFERENCES starred_proteins (starred_proteins_id);


ALTER TABLE search_engine ADD FOREIGN KEY search_engine_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE search_engine ADD FOREIGN KEY search_engine_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);

ALTER TABLE search_metadata DROP FOREIGN KEY `search_metadata_ibfk_1`; #swift_search_definition-swift_search_definition_id
ALTER TABLE search_metadata ADD FOREIGN KEY search_metadata_ibfk_1 (swift_search_definition_id) REFERENCES swift_search_definition (swift_search_definition_id);

ALTER TABLE search_parameters DROP FOREIGN KEY `FK94CF18E152E12E42`; #mod_set-mod_set_id
ALTER TABLE search_parameters DROP FOREIGN KEY `FK94CF18E1552CD370`; #scaffold_settings-scaffold_settings_id
ALTER TABLE search_parameters DROP FOREIGN KEY `FK94CF18E168F5744A`; #mod_set-mod_set_id
ALTER TABLE search_parameters DROP FOREIGN KEY `FK94CF18E16C6B9995`; #protease-protease_id
ALTER TABLE search_parameters DROP FOREIGN KEY `FK94CF18E16F599AF9`; #instrument-instrument_id
ALTER TABLE search_parameters DROP FOREIGN KEY `FK94CF18E18A124193`; #curation-curation_id
ALTER TABLE search_parameters DROP FOREIGN KEY `FK94CF18E1B41BEAD`; #extract_msn_settings-extract_msn_settings_id
ALTER TABLE search_parameters ADD FOREIGN KEY search_parameters_ibfk_1 (variable_mods) REFERENCES mod_set (mod_set_id);
ALTER TABLE search_parameters ADD FOREIGN KEY search_parameters_ibfk_2 (scaffold_settings) REFERENCES scaffold_settings (scaffold_settings_id);
ALTER TABLE search_parameters ADD FOREIGN KEY search_parameters_ibfk_3 (fixed_mods) REFERENCES mod_set (mod_set_id);
ALTER TABLE search_parameters ADD FOREIGN KEY search_parameters_ibfk_4 (protease) REFERENCES protease (protease_id);
ALTER TABLE search_parameters ADD FOREIGN KEY search_parameters_ibfk_5 (instrument) REFERENCES instrument (instrument_id);
ALTER TABLE search_parameters ADD FOREIGN KEY search_parameters_ibfk_6 (curation) REFERENCES curation (curation_id);
ALTER TABLE search_parameters ADD FOREIGN KEY search_parameters_ibfk_7 (extract_msn_settings) REFERENCES extract_msn_settings (extract_msn_settings_id);

ALTER TABLE search_result DROP FOREIGN KEY `FK54FD3294C1265750`; #protein_group_list-protein_group_list_id
ALTER TABLE search_result DROP FOREIGN KEY `FK54FD3294FA17B0F`; #tandem_mass_spec_sample-tandem_mass_spec_sample_id
ALTER TABLE search_result ADD FOREIGN KEY search_result_ibfk_1 (protein_group_list_id) REFERENCES protein_group_list (protein_group_list_id);
ALTER TABLE search_result ADD FOREIGN KEY search_result_ibfk_2 (tandem_mass_spec_sample_id) REFERENCES tandem_mass_spec_sample (tandem_mass_spec_sample_id);

ALTER TABLE search_result_list_members DROP FOREIGN KEY `FKD839AA4331BFFCCA`; #search_result_list-search_result_list_id
ALTER TABLE search_result_list_members DROP FOREIGN KEY `FKD839AA43AE55EA81`; #search_result-search_result_id
ALTER TABLE search_result_list_members ADD FOREIGN KEY search_result_list_members_ibfk_1 (search_result_list_id) REFERENCES search_result_list (search_result_list_id);
ALTER TABLE search_result_list_members ADD FOREIGN KEY search_result_list_members_ibfk_2 (search_result_id) REFERENCES search_result (search_result_id);

ALTER TABLE swift_search_definition DROP FOREIGN KEY `ssd_pepr_fk`; #peptide_report-peptide_report_id
ALTER TABLE swift_search_definition DROP FOREIGN KEY `ssd_sp_fk`; #search_parameters-search_parameter_id
ALTER TABLE swift_search_definition DROP FOREIGN KEY `ssd_sqa_fk`; #spectrum_qa-spectrum_qa_id
ALTER TABLE swift_search_definition DROP FOREIGN KEY `ssd_user_fk`; #workflow_user-workflow_user_id
ALTER TABLE swift_search_definition ADD FOREIGN KEY swift_search_definition_ibfk_1 (peptide_report) REFERENCES peptide_report (peptide_report_id);
ALTER TABLE swift_search_definition ADD FOREIGN KEY swift_search_definition_ibfk_2 (search_parameters) REFERENCES search_parameters (search_parameter_id);
ALTER TABLE swift_search_definition ADD FOREIGN KEY swift_search_definition_ibfk_3 (spectrum_qa) REFERENCES spectrum_qa (spectrum_qa_id);
ALTER TABLE swift_search_definition ADD FOREIGN KEY swift_search_definition_ibfk_4 (owner) REFERENCES workflow_user (workflow_user_id);

ALTER TABLE task DROP FOREIGN KEY `FK3635858BC37886`; #task_state-task_state_id
ALTER TABLE task DROP FOREIGN KEY `FK363585FE2045B8`; #transaction-transaction_id
ALTER TABLE task ADD FOREIGN KEY task_ibfk_1 (task_state) REFERENCES task_state (task_state_id);
ALTER TABLE task ADD FOREIGN KEY task_ibfk_2 (transaction_id) REFERENCES `transaction` (transaction_id);

ALTER TABLE `transaction` DROP FOREIGN KEY `FK7FA0D2DE484A3C16`; #workflow_user-workflow_user_id
ALTER TABLE `transaction` ADD FOREIGN KEY transaction_ibfk_1 (submitting_user) REFERENCES workflow_user (workflow_user_id);

ALTER TABLE user_preferences DROP FOREIGN KEY `FK199BD0841536D8C1`; #workflow_user-workflow_user_id
ALTER TABLE user_preferences ADD FOREIGN KEY user_preferences_ibfk_1 (user_id) REFERENCES workflow_user (workflow_user_id);

ALTER TABLE workflow_user DROP FOREIGN KEY `FK4411CC4B2D806023`; #change_audit-change_audit_id
ALTER TABLE workflow_user DROP FOREIGN KEY `FK4411CC4B6925EB34`; #change_audit-change_audit_id
ALTER TABLE workflow_user ADD FOREIGN KEY workflow_user_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE workflow_user ADD FOREIGN KEY workflow_user_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);

ALTER TABLE `protein_group` DROP FOREIGN KEY `FK842542C3DC37B774`;
ALTER TABLE `protein_group` ADD CONSTRAINT `protein_group_ibfk_2` FOREIGN KEY (`peptide_spectrum_match_list_id`) REFERENCES `peptide_spectrum_match_list` (`peptide_spectrum_match_list_id`);
