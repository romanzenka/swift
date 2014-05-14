-- Migrate the atlas_prod database, table by table, into this schema

SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO user_preferences (user_id, preference_value, preference_name)
  SELECT
    `user_preferences`.`user_id`,
    `user_preferences`.`preference_value`,
    `user_preferences`.`preference_name`
  FROM `atlas_prod`.`user_preferences`
  ORDER BY user_id;

INSERT INTO `change_audit` (`change_audit_id`, `reason`, `change_date`)
  SELECT
    `change_audit_id`,
    `reason`,
    `change_date`
  FROM `atlas_prod`.`change_audit`
  ORDER BY change_audit_id;

INSERT INTO `search_engine` (`search_engine_id`, `code`, `creation`, `deletion`)
  SELECT
    `search_engine_id`,
    `code`,
    `creation`,
    `deletion`
  FROM `atlas_prod`.`search_engine`
  ORDER BY search_engine_id;

INSERT INTO `workflow_user`
(`workflow_user_id`, `first_name`, `last_name`, `user_password`, `user_name`, `initials`, `rights`, `creation`, `deletion`)
  SELECT
    `workflow_user_id`,
    `first_name`,
    `last_name`,
    `user_password`,
    `user_name`,
    `initials`,
    `rights`,
    `creation`,
    `deletion`
  FROM `atlas_prod`.`workflow_user`
  ORDER BY workflow_user_id;

INSERT INTO `protease` (`protease_id`, `name`, `rn`, `rn_minus_1`, `creation`, `deletion`)
  SELECT
    `protease_id`,
    `name`,
    `rn`,
    `rn_minus_1`,
    `creation`,
    `deletion`
  FROM `atlas_prod`.`protease`
  ORDER BY protease_id;

INSERT INTO `ion_series` (`ion_series_id`, `name`, `deletion`, `creation`)
  SELECT
    `ion_series_id`,
    `name`,
    `deletion`,
    `creation`
  FROM `atlas_prod`.ion_series
  ORDER BY ion_series_id;

INSERT INTO `instrument` (`instrument_id`, `name`, `mascot_name`, `creation`, `deletion`)
  SELECT
    `instrument_id`,
    `name`,
    `mascot_name`,
    `creation`,
    `deletion`
  FROM `atlas_prod`.instrument
  ORDER BY instrument_id;

INSERT INTO `instrument_series` (`instrument_id`, `ion_series_id`)
  SELECT
    `instrument_id`,
    `ion_series_id`
  FROM `atlas_prod`.instrument_series
  ORDER BY instrument_id, ion_series_id;

INSERT INTO `peptide_report` (`peptide_report_id`)
  SELECT
    `peptide_report_id`
  FROM `atlas_prod`.peptide_report
  ORDER BY peptide_report_id;

INSERT INTO `tandem_mass_spec_sample` (`tandem_mass_spec_sample_id`, `file`, `last_modified`,
                                       `ms1_spectra`, `ms2_spectra`, `ms3_plus_spectra`, `instrument_name`,
                                       `instrument_serial_number`, `start_time`, `run_time_seconds`, `comment`,
                                       `sample_information`)
  SELECT
    `tandem_mass_spec_sample_id`,
    `file`,
    `last_modified`,
    `ms1_spectra`,
    `ms2_spectra`,
    `ms3_plus_spectra`,
    `instrument_name`,
    `instrument_serial_number`,
    `start_time`,
    `run_time_seconds`,
    `comment`,
    `sample_information`
  FROM
    `atlas_prod`.tandem_mass_spec_sample
  ORDER BY tandem_mass_spec_sample_id;

INSERT INTO `starred_proteins` (`starred_proteins_id`, `starred`, `protein_delimiter`, `regular_expression`, `match_name`)
  SELECT
    `starred_proteins_id`,
    `starred`,
    `protein_delimiter`,
    `regular_expression`,
    `match_name`
  FROM
    `atlas_prod`.starred_proteins
  ORDER BY starred_proteins_id;

INSERT INTO `enabled_engines` (`enabled_engines_id`)
  SELECT
    enabled_engines_id
  FROM
    `atlas_prod`.enabled_engines
  ORDER BY enabled_engines_id;

INSERT INTO `enabled_engines_set` (`set_id`, `search_engine_id`)
  SELECT
    `set_id`,
    `search_engine_id`
  FROM
    `atlas_prod`.enabled_engines_set
  ORDER BY search_engine_id;

INSERT INTO `task_state` (`task_state_id`, `description`)
  SELECT
    `task_state_id`,
    `description`
  FROM
    `atlas_prod`.task_state
  ORDER BY task_state_id;

INSERT INTO `spectrum_qa` (`spectrum_qa_id`, `engine`, `param_file_path`)
  SELECT
    `spectrum_qa_id`,
    `engine`,
    `param_file_path`
  FROM
    `atlas_prod`.spectrum_qa
  ORDER BY spectrum_qa_id;

INSERT INTO `mod`
(`mod_id`,
 `title`,
 `full_name`,
 `record_id`,
 `composition`,
 `deletion`,
 `mass_mono`,
 `mass_average`,
 `creation`)
  SELECT
    `mod_id`,
    `title`,
    `full_name`,
    `record_id`,
    `composition`,
    `deletion`,
    `mass_mono`,
    `mass_average`,
    `creation`
  FROM
    `atlas_prod`.`mod`
  ORDER BY mod_id;

INSERT INTO `mod_alt_names`
(`alt_name_id`,
 `alt_name`)
  SELECT
    `alt_name_id`,
    `alt_name`
  FROM
    `atlas_prod`.`mod_alt_names`
  ORDER BY alt_name_id;

INSERT INTO `mod_specificity`
(`specificity_id`,
 `mod_id`,
 `hidden`,
 `site`,
 `terminus`,
 `protein_only`,
 `classification`,
 `specificity_group`,
 `comments`)
  SELECT
    `specificity_id`,
    `mod_id`,
    `hidden`,
    `site`,
    `terminus`,
    `protein_only`,
    `classification`,
    `specificity_group`,
    `comments`
  FROM
    `atlas_prod`.mod_specificity
  ORDER BY specificity_id;

INSERT INTO `mod_set`
(`mod_set_id`)
  SELECT
    `mod_set_id`
  FROM
    `atlas_prod`.mod_set
  ORDER BY mod_set_id;

INSERT INTO `mod_set_specificities`
(`set_id`,
 `specificity_id`)
  SELECT
    `set_id`,
    `specificity_id`
  FROM
    `atlas_prod`.mod_set_specificities
  ORDER BY set_id;

INSERT INTO `extract_msn_settings`
(`extract_msn_settings_id`,
 `command_line_switches`)
  SELECT
    `extract_msn_settings_id`,
    `command_line_switches`
  FROM
    `atlas_prod`.extract_msn_settings
  ORDER BY extract_msn_settings_id;

INSERT INTO `scaffold_settings`
(`scaffold_settings_id`,
 `protein_probability`,
 `peptide_probability`,
 `minimum_peptide_count`,
 `minimum_non_tryptic_terminii`,
 `starred_proteins`,
 `save_only_identified_spectra`,
 `save_no_spectra`,
 `connect_to_ncbi`,
 `annotate_with_goa`)
  SELECT
    `scaffold_settings_id`,
    `protein_probability`,
    `peptide_probability`,
    `minimum_peptide_count`,
    `minimum_non_tryptic_terminii`,
    `starred_proteins`,
    `save_only_identified_spectra`,
    `save_no_spectra`,
    `connect_to_ncbi`,
    `annotate_with_goa`
  FROM
    `atlas_prod`.scaffold_settings
  ORDER BY scaffold_settings_id;

INSERT INTO `curation_header_transform`
(`header_transform_id`,
 `name`,
 `group_pattern`,
 `substitute_pattern`,
 `is_common`)
  SELECT
    `header_transform_id`,
    `name`,
    `group_pattern`,
    `substitute_pattern`,
    `is_common`
  FROM
    `atlas_prod`.curation_header_transform
  ORDER BY header_transform_id;

INSERT INTO `curation_data_source`
(`data_source_id`,
 `name`,
 `url`,
 `is_common`,
 `auto_transform`)
  SELECT
    `data_source_id`,
    `name`,
    `url`,
    `is_common`,
    `auto_transform`
  FROM
    `atlas_prod`.curation_data_source
  ORDER BY data_source_id;

INSERT INTO `source_database_archive`
(`source_id`,
 `source_url`,
 `source_path`,
 `server_creation_date`,
 `download_date`)
  SELECT
    `source_id`,
    `source_url`,
    `source_path`,
    `server_creation_date`,
    `download_date`
  FROM
    `atlas_prod`.source_database_archive
  ORDER BY source_id;

INSERT INTO `curation_step`
(`step_id`,
 `last_run_completion_count`)
  SELECT
    `step_id`,
    `last_run_completion_count`
  FROM
    `atlas_prod`.curation_step
  ORDER BY step_id;

INSERT INTO `curation_step_database_upload`
(`upload_id`,
 `server_path`,
 `client_path`,
 `md5_checksum`)
  SELECT
    `upload_id`,
    `server_path`,
    `client_path`,
    `md5_checksum`
  FROM
    `atlas_prod`.curation_step_database_upload
  ORDER BY upload_id;

INSERT INTO `curation_step_header_filter`
(`header_filter_id`,
 `criteria_string`,
 `text_mode`,
 `match_mode`)
  SELECT
    `header_filter_id`,
    `criteria_string`,
    `text_mode`,
    `match_mode`
  FROM
    `atlas_prod`.curation_step_header_filter
  ORDER BY header_filter_id;

INSERT INTO `curation_step_header_transform`
(`header_transform_id`,
 `description`,
 `match_pattern`,
 `substitution_pattern`)
  SELECT
    `header_transform_id`,
    `description`,
    `match_pattern`,
    `substitution_pattern`
  FROM
    `atlas_prod`.curation_step_header_transform
  ORDER BY header_transform_id;

INSERT INTO `curation_step_make_decoy`
(`sequence_manipulation_id`,
 `overwrite_mode`,
 `manipulator_type`)
  SELECT
    `sequence_manipulation_id`,
    `overwrite_mode`,
    `manipulator_type`
  FROM
    `atlas_prod`.curation_step_make_decoy
  ORDER BY sequence_manipulation_id;

INSERT INTO `curation_step_manual_inclusion`
(`manual_inclusion_id`,
 `inclusion_header`,
 `inclusion_sequence`)
  SELECT
    `manual_inclusion_id`,
    `inclusion_header`,
    `inclusion_sequence`
  FROM
    `atlas_prod`.curation_step_manual_inclusion
  ORDER BY manual_inclusion_id;

INSERT INTO `curation_step_new_db`
(`new_database_id`,
 `url`,
 `source_db`)
  SELECT
    `new_database_id`,
    `url`,
    `source_db`
  FROM
    `atlas_prod`.curation_step_new_db
  ORDER BY new_database_id;

INSERT INTO `curation`
(`curation_id`,
 `short_name`,
 `first_run_date`,
 `curated_filename`,
 `title`,
 `email`,
 `run_date`,
 `deployment_date`,
 `notes`,
 `decoy_regex`,
 `creation`,
 `deletion`)
  SELECT
    `curation_id`,
    `short_name`,
    `first_run_date`,
    `curated_filename`,
    `title`,
    `email`,
    `run_date`,
    `deployment_date`,
    `notes`,
    `decoy_regex`,
    `creation`,
    `deletion`
  FROM
    `atlas_prod`.curation
  ORDER BY curation_id;

INSERT INTO `curation_step_list`
(`curation_id`,
 `step_id`,
 `sort_order`)
  SELECT
    `curation_id`,
    `step_id`,
    `sort_order`
  FROM
    `atlas_prod`.curation_step_list
  ORDER BY curation_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`,
 `mass`,
 `sequence`,
 `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM
    `atlas_prod`.protein_sequence
  WHERE 0 < protein_sequence_id AND protein_sequence_id <= 100000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`,
 `mass`,
 `sequence`,
 `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM
    `atlas_prod`.protein_sequence
  WHERE 100000 < protein_sequence_id AND protein_sequence_id <= 200000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`,
 `mass`,
 `sequence`,
 `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM
    `atlas_prod`.protein_sequence
  WHERE 200000 < protein_sequence_id AND protein_sequence_id <= 300000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`,
 `mass`,
 `sequence`,
 `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM
    `atlas_prod`.protein_sequence
  WHERE 300000 < protein_sequence_id AND protein_sequence_id <= 400000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`,
 `mass`,
 `sequence`,
 `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM
    `atlas_prod`.protein_sequence
  WHERE 400000 < protein_sequence_id AND protein_sequence_id <= 500000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`,
 `mass`,
 `sequence`,
 `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM
    `atlas_prod`.protein_sequence
  WHERE 500000 < protein_sequence_id AND protein_sequence_id <= 600000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`,
 `mass`,
 `sequence`,
 `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM
    `atlas_prod`.protein_sequence
  WHERE 600000 < protein_sequence_id AND protein_sequence_id <= 700000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`, `mass`, `sequence`, `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM `atlas_prod`.protein_sequence
  WHERE 700000 < protein_sequence_id AND protein_sequence_id <= 800000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`, `mass`, `sequence`, `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM `atlas_prod`.protein_sequence
  WHERE 800000 < protein_sequence_id AND protein_sequence_id <= 900000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`, `mass`, `sequence`, `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM `atlas_prod`.protein_sequence
  WHERE 900000 < protein_sequence_id AND protein_sequence_id <= 1000000
  ORDER BY protein_sequence_id;

INSERT INTO `protein_sequence`
(`protein_sequence_id`, `mass`, `sequence`, `accession_number`)
  SELECT
    `protein_sequence_id`,
    `mass`,
    `sequence`,
    `accession_number`
  FROM `atlas_prod`.protein_sequence
  WHERE 1000000 < protein_sequence_id
  ORDER BY protein_sequence_id;

INSERT INTO `protein_database_entry`
(`protein_database_entry_id`,
 `curation_id`,
 `accession_number`,
 `description`,
 `protein_sequence_id`)
  SELECT
    `protein_database_entry_id`,
    `curation_id`,
    `accession_number`,
    `description`,
    `protein_sequence_id`
  FROM
    `atlas_prod`.protein_database_entry
  ORDER BY protein_database_entry_id;

INSERT INTO `protein_sequence_list`
(`protein_sequence_list_id`,
 `hash`)
  SELECT
    `protein_sequence_list_id`,
    `hash`
  FROM
    `atlas_prod`.protein_sequence_list
  ORDER BY protein_sequence_list_id;

INSERT INTO `protein_sequence_list_members`
(`protein_sequence_list_id`,
 `protein_sequence_id`)
  SELECT
    `protein_sequence_list_id`,
    `protein_sequence_id`
  FROM
    `atlas_prod`.protein_sequence_list_members
  ORDER BY protein_sequence_list_id;


INSERT INTO `search_parameters`
(`search_parameter_id`,
 `curation`,
 `protease`,
 `missed_cleavages`,
 `fixed_mods`,
 `variable_mods`,
 `peptide_tolerance_value`,
 `peptide_tolerance_unit`,
 `fragment_tolerance_value`,
 `fragment_tolerance_unit`,
 `instrument`,
 `extract_msn_settings`,
 `scaffold_settings`)
  SELECT
    `search_parameter_id`,
    `curation`,
    `protease`,
    `missed_cleavages`,
    `fixed_mods`,
    `variable_mods`,
    `peptide_tolerance_value`,
    `peptide_tolerance_unit`,
    `fragment_tolerance_value`,
    `fragment_tolerance_unit`,
    `instrument`,
    `extract_msn_settings`,
    `scaffold_settings`
  FROM
    `atlas_prod`.search_parameters
  ORDER BY search_parameter_id;

INSERT INTO `saved_parameters`
(`saved_parameter_id`,
 `name`,
 `owner`,
 `parameters`,
 `creation`,
 `deletion`)
  SELECT
    `saved_parameter_id`,
    `name`,
    `owner`,
    `parameters`,
    `creation`,
    `deletion`
  FROM
    `atlas_prod`.saved_parameters
  ORDER BY saved_parameter_id;

INSERT INTO `protein_group`
(`protein_group_id`,
 `protein_sequence_list_id`,
 `protein_identification_probability`,
 `unique_peptides`,
 `unique_spectra`,
 `total_spectra`,
 `percentage_total_spectra`,
 `percentage_sequence_coverage`)
  SELECT
    `protein_group_id`,
    `protein_sequence_list_id`,
    `protein_identification_probability`,
    `unique_peptides`,
    `unique_spectra`,
    `total_spectra`,
    `percentage_total_spectra`,
    `percentage_sequence_coverage`
  FROM
    `atlas_prod`.protein_group
  ORDER BY protein_group_id;

INSERT INTO `protein_group_list`
(`protein_group_list_id`,
 `hash`)
  SELECT
    `protein_group_list_id`,
    `hash`
  FROM
    `atlas_prod`.protein_group_list
  ORDER BY protein_group_list_id;

INSERT INTO `protein_group_list_members`
(`protein_group_list_id`,
 `protein_group_id`)
  SELECT
    `protein_group_list_id`,
    `protein_group_id`
  FROM
    `atlas_prod`.protein_group_list_members
  ORDER BY protein_group_list_id;

INSERT INTO `swift_search_definition`
(`swift_search_definition_id`,
 `title`,
 `owner`,
 `output_folder`,
 `spectrum_qa`,
 `peptide_report`,
 `extract_msn_settings`,
 `scaffold_settings`,
 `public_mgf_files`,
 `public_search_files`,
 `search_parameters`)
  SELECT
    `swift_search_definition_id`,
    `title`,
    `owner`,
    `output_folder`,
    `spectrum_qa`,
    `peptide_report`,
    `extract_msn_settings`,
    `scaffold_settings`,
    `public_mgf_files`,
    `public_search_files`,
    `search_parameters`
  FROM
    `atlas_prod`.swift_search_definition
  ORDER BY swift_search_definition_id;

INSERT INTO `file_search`
(`file_search_id`,
 `input_file`,
 `biological_sample`,
 `category_name`,
 `experiment`,
 `search_parameters`,
 `enabled_engines`,
 `input_files_id`,
 `sort_order`)
  SELECT
    `file_search_id`,
    `input_file`,
    `biological_sample`,
    `category_name`,
    `experiment`,
    `search_parameters`,
    `enabled_engines`,
    `input_files_id`,
    `sort_order`
  FROM
    `atlas_prod`.file_search
  ORDER BY file_search_id;

INSERT INTO `search_result`
(`search_result_id`,
 `protein_group_list_id`,
 `tandem_mass_spec_sample_id`)
  SELECT
    `search_result_id`,
    `protein_group_list_id`,
    `tandem_mass_spec_sample_id`
  FROM
    `atlas_prod`.search_result
  ORDER BY search_result_id;

INSERT INTO `search_result_list`
(`search_result_list_id`,
 `hash`)
  SELECT
    `search_result_list_id`,
    `hash`
  FROM
    `atlas_prod`.search_result_list
  ORDER BY search_result_list_id;

INSERT INTO `search_result_list_members`
(`search_result_list_id`,
 `search_result_id`)
  SELECT
    `search_result_list_id`,
    `search_result_id`
  FROM
    `atlas_prod`.search_result_list_members
  ORDER BY search_result_list_id;

INSERT INTO `biological_sample`
(`biological_sample_id`,
 `category`,
 `sample_name`,
 `search_result_list_id`)
  SELECT
    `biological_sample_id`,
    `category`,
    `sample_name`,
    `search_result_list_id`
  FROM
    `atlas_prod`.biological_sample
  ORDER BY biological_sample_id;

INSERT INTO `biological_sample_list`
(`biological_sample_list_id`,
 `hash`)
  SELECT
    `biological_sample_list_id`,
    `hash`
  FROM
    `atlas_prod`.biological_sample_list
  ORDER BY biological_sample_list_id;

INSERT INTO `biological_sample_list_members`
(`biological_sample_list_id`,
 `biological_sample_id`)
  SELECT
    `biological_sample_list_id`,
    `biological_sample_id`
  FROM
    `atlas_prod`.biological_sample_list_members
  ORDER BY biological_sample_list_id, biological_sample_id;

INSERT INTO `transaction`
(`transaction_id`,
 `title`,
 `submitting_user`,
 `xml_def_file`,
 `swift_search`,
 `start_timestamp`,
 `end_timestamp`,
 `error_code`,
 `error_message`,
 `num_tasks`,
 `tasks_with_warning`,
 `tasks_failed`,
 `tasks_completed`,
 `hidden`)
  SELECT
    `transaction_id`,
    `title`,
    `submitting_user`,
    `xml_def_file`,
    `swift_search`,
    `start_timestamp`,
    `end_timestamp`,
    `error_code`,
    `error_message`,
    `num_tasks`,
    `tasks_with_warning`,
    `tasks_failed`,
    `tasks_completed`,
    `hidden`
  FROM
    `atlas_prod`.`transaction`
  ORDER BY transaction_id;

INSERT INTO `task`
(`task_id`,
 `task_name`,
 `queue_timestamp`,
 `start_timestamp`,
 `end_timestamp`,
 `error_code`,
 `error_message`,
 `exception`,
 `transaction_id`,
 `task_state`,
 `description_long`,
 `grid_job_id`,
 `out_log`,
 `err_log`,
 `host`,
 `percent_done`)
  SELECT
    `task_id`,
    `task_name`,
    `queue_timestamp`,
    `start_timestamp`,
    `end_timestamp`,
    `error_code`,
    `error_message`,
    `exception`,
    `transaction_id`,
    `task_state`,
    `description_long`,
    `grid_job_id`,
    `out_log`,
    `err_log`,
    `host`,
    `percent_done`
  FROM
    `atlas_prod`.task
  ORDER BY task_id;

INSERT INTO `report`
(`report_id`,
 `report_file`,
 `date_created`,
 `transaction_id`)
  SELECT
    `report_id`,
    `report_file`,
    `date_created`,
    `transaction_id`
  FROM
    `atlas_prod`.report
  ORDER BY report_id;

INSERT INTO `analysis`
(`analysis_id`,
 `report_id`,
 `scaffold_version`,
 `analysis_date`,
 `biological_sample_list_id`)
  SELECT
    `analysis_id`,
    `report_id`,
    `scaffold_version`,
    `analysis_date`,
    `biological_sample_list_id`
  FROM
    `atlas_prod`.analysis
  ORDER BY analysis_id;

SET UNIQUE_CHECKS = 1;
SET FOREIGN_KEY_CHECKS = 1;


-- @UNDO
SET foreign_key_checks = 0;

TRUNCATE TABLE `user_preferences`;
TRUNCATE TABLE `change_audit`;
TRUNCATE TABLE `search_engine`;
TRUNCATE TABLE `workflow_user`;
TRUNCATE TABLE `protease`;
TRUNCATE TABLE `ion_series`;
TRUNCATE TABLE `instrument`;
TRUNCATE TABLE `instrument_series`;
TRUNCATE TABLE `peptide_report`;
TRUNCATE TABLE `tandem_mass_spec_sample`;
TRUNCATE TABLE `starred_proteins`;
TRUNCATE TABLE `enabled_engines`;
TRUNCATE TABLE `enabled_engines_set`;
TRUNCATE TABLE `task_state`;
TRUNCATE TABLE `spectrum_qa`;
TRUNCATE TABLE `mod`;
TRUNCATE TABLE `mod_alt_names`;
TRUNCATE TABLE `instrument_series`;
TRUNCATE TABLE `mod_specificity`;
TRUNCATE TABLE `localized_modification`;
TRUNCATE TABLE `mod_set`;
TRUNCATE TABLE `mod_set_specificities`;
TRUNCATE TABLE `extract_msn_settings`;
TRUNCATE TABLE `scaffold_settings`;
TRUNCATE TABLE `curation_header_transform`;
TRUNCATE TABLE `curation_data_source`;
TRUNCATE TABLE `source_database_archive`;
TRUNCATE TABLE `curation_step`;
TRUNCATE TABLE `curation_step_database_upload`;
TRUNCATE TABLE `curation_step_header_filter`;
TRUNCATE TABLE `curation_step_header_transform`;
TRUNCATE TABLE `curation_step_make_decoy`;
TRUNCATE TABLE `curation_step_manual_inclusion`;
TRUNCATE TABLE `curation_step_new_db`;
TRUNCATE TABLE `curation`;
TRUNCATE TABLE `curation_step_list`;
TRUNCATE TABLE `protein_sequence`;
TRUNCATE TABLE `protein_database_entry`;
TRUNCATE TABLE `protein_sequence_list`;
TRUNCATE TABLE `protein_sequence_list_members`;
TRUNCATE TABLE `search_parameters`;
TRUNCATE TABLE `saved_parameters`;
TRUNCATE TABLE `protein_group`;
TRUNCATE TABLE `protein_group_list`;
TRUNCATE TABLE `protein_group_list_members`;
TRUNCATE TABLE `swift_search_definition`;
TRUNCATE TABLE `file_search`;
TRUNCATE TABLE `search_result`;
TRUNCATE TABLE `search_result_list`;
TRUNCATE TABLE `search_result_list_members`;
TRUNCATE TABLE `biological_sample`;
TRUNCATE TABLE `biological_sample_list`;
TRUNCATE TABLE `biological_sample_list_members`;
TRUNCATE TABLE `transaction`;
TRUNCATE TABLE `task`;
TRUNCATE TABLE `report`;
TRUNCATE TABLE `analysis`;

SET foreign_key_checks = 1;

