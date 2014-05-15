-- Initial version of Swift's database as deployed to Atlas Production in 2013
-- This is the basis on which we build extra features

CREATE TABLE analysis
(
  analysis_id               INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  report_id                 BIGINT,
  scaffold_version          VARCHAR(20),
  analysis_date             DATETIME,
  biological_sample_list_id INT,
  UNIQUE KEY `uniqueness` (`report_id`, `scaffold_version`, `analysis_date`, `biological_sample_list_id`)
);
CREATE TABLE biological_sample
(
  biological_sample_id  INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  category              VARCHAR(200),
  sample_name           VARCHAR(200),
  search_result_list_id INT,
  UNIQUE KEY `uniqueness` (`category`, `sample_name`, `search_result_list_id`)
);
CREATE TABLE biological_sample_list
(
  biological_sample_list_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  hash                      BIGINT          NOT NULL
);
CREATE TABLE biological_sample_list_members
(
  biological_sample_list_id INT NOT NULL,
  biological_sample_id      INT NOT NULL,
  PRIMARY KEY (biological_sample_list_id, biological_sample_id)
);
CREATE TABLE change_audit
(
  change_audit_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  reason          LONGTEXT,
  change_date     DATETIME
);
CREATE TABLE curation
(
  curation_id      INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  short_name       VARCHAR(64)     NOT NULL,
  first_run_date   DATETIME,
  curated_filename VARCHAR(255),
  title            VARCHAR(255),
  email            VARCHAR(32),
  run_date         DATETIME,
  deployment_date  DATETIME,
  notes            VARCHAR(255),
  decoy_regex      VARCHAR(200),
  creation         INT,
  deletion         INT
);
CREATE TABLE curation_data_source
(
  data_source_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name           VARCHAR(50),
  url            VARCHAR(255),
  is_common      CHAR(1),
  auto_transform INT,
  UNIQUE KEY `name` (`name`)
);
CREATE TABLE curation_header_transform
(
  header_transform_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name                VARCHAR(50),
  group_pattern       VARCHAR(255),
  substitute_pattern  VARCHAR(255),
  is_common           CHAR(1)
);
CREATE TABLE curation_step
(
  step_id                   INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  last_run_completion_count INT
);
CREATE TABLE curation_step_database_upload
(
  upload_id    INT PRIMARY KEY NOT NULL,
  server_path  VARCHAR(255),
  client_path  VARCHAR(255),
  md5_checksum TINYBLOB
);
CREATE TABLE curation_step_header_filter
(
  header_filter_id INT PRIMARY KEY NOT NULL,
  criteria_string  LONGTEXT,
  text_mode        VARCHAR(255),
  match_mode       VARCHAR(255)
);
CREATE TABLE curation_step_header_transform
(
  header_transform_id  INT PRIMARY KEY NOT NULL,
  description          VARCHAR(255),
  match_pattern        VARCHAR(255),
  substitution_pattern VARCHAR(255)
);
CREATE TABLE curation_step_list
(
  curation_id INT NOT NULL,
  step_id     INT NOT NULL,
  sort_order  INT NOT NULL,
  PRIMARY KEY (curation_id, sort_order)
);
CREATE TABLE curation_step_make_decoy
(
  sequence_manipulation_id INT PRIMARY KEY NOT NULL,
  overwrite_mode           BIT,
  manipulator_type         INT
);
CREATE TABLE curation_step_manual_inclusion
(
  manual_inclusion_id INT PRIMARY KEY NOT NULL,
  inclusion_header    LONGTEXT,
  inclusion_sequence  LONGTEXT
);
CREATE TABLE curation_step_new_db
(
  new_database_id INT PRIMARY KEY NOT NULL,
  url             LONGTEXT,
  source_db       INT
);
CREATE TABLE enabled_engines
(
  enabled_engines_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT
);
CREATE TABLE enabled_engines_set
(
  set_id           INT NOT NULL,
  search_engine_id INT NOT NULL,
  PRIMARY KEY (set_id, search_engine_id)
);
CREATE TABLE extract_msn_settings
(
  extract_msn_settings_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  command_line_switches   VARCHAR(200),
  UNIQUE KEY `command_line_switches` (`command_line_switches`)
);
CREATE TABLE file_search
(
  file_search_id    INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  input_file        VARCHAR(255),
  biological_sample VARCHAR(128),
  category_name     VARCHAR(128),
  experiment        VARCHAR(128),
  search_parameters INT,
  enabled_engines   INT,
  input_files_id    INT,
  sort_order        INT,
  UNIQUE KEY `input_file` (`input_file`, `biological_sample`, `category_name`, `experiment`, `search_parameters`, `enabled_engines`)
);
CREATE TABLE instrument
(
  instrument_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name          VARCHAR(20),
  mascot_name   VARCHAR(20),
  creation      INT,
  deletion      INT
);
CREATE TABLE instrument_series
(
  instrument_id INT NOT NULL,
  ion_series_id INT NOT NULL,
  PRIMARY KEY (instrument_id, ion_series_id)
);
CREATE TABLE ion_series
(
  ion_series_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name          VARCHAR(20),
  deletion      INT,
  creation      INT,
  UNIQUE KEY `name` (`name`, `deletion`)
);
CREATE TABLE `mod`
(
  mod_id       INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  title        VARCHAR(150),
  full_name    VARCHAR(150),
  record_id    INT,
  composition  VARCHAR(150),
  deletion     INT,
  mass_mono    DOUBLE,
  mass_average DOUBLE,
  creation     INT,
  UNIQUE KEY `title` (`title`, `full_name`, `record_id`, `composition`, `deletion`)
);
CREATE TABLE mod_alt_names
(
  alt_name_id INT NOT NULL,
  alt_name    VARCHAR(255)
);
CREATE TABLE mod_set
(
  mod_set_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT
);
CREATE TABLE mod_set_specificities
(
  set_id         INT NOT NULL,
  specificity_id INT NOT NULL,
  PRIMARY KEY (set_id, specificity_id)
);
CREATE TABLE mod_specificity
(
  specificity_id    INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  mod_id            INT             NOT NULL,
  hidden            BIT,
  site              CHAR(1),
  terminus          VARCHAR(255),
  protein_only      BIT,
  classification    VARCHAR(200),
  specificity_group INT,
  comments          LONGTEXT
);
CREATE TABLE peptide_report
(
  peptide_report_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT
);
CREATE TABLE protease
(
  protease_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name        VARCHAR(50),
  rn          VARCHAR(40),
  rn_minus_1  VARCHAR(40),
  creation    INT,
  deletion    INT
);
CREATE TABLE protein_database_entry
(
  protein_database_entry_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  curation_id               INT,
  accession_number          VARCHAR(80)     NOT NULL,
  description               LONGTEXT,
  protein_sequence_id       INT             NOT NULL,
  KEY `lookup_sequence` (`curation_id`, `accession_number`, `protein_sequence_id`),
  KEY `lookup_accession` (`protein_sequence_id`, `curation_id`, `accession_number`)
);
CREATE TABLE protein_group
(
  protein_group_id                   INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  protein_sequence_list_id           INT,
  protein_identification_probability DOUBLE,
  unique_peptides                    INT,
  unique_spectra                     INT,
  total_spectra                      INT,
  percentage_total_spectra           DOUBLE,
  percentage_sequence_coverage       DOUBLE
);
CREATE TABLE protein_group_list
(
  protein_group_list_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  hash                  BIGINT          NOT NULL,
  KEY `hash` (`hash`, `protein_group_list_id`)
);
CREATE TABLE protein_group_list_members
(
  protein_group_list_id INT NOT NULL,
  protein_group_id      INT NOT NULL
);
CREATE TABLE protein_sequence
(
  protein_sequence_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  mass                DOUBLE,
  sequence            LONGTEXT,
  accession_number    VARCHAR(80),
  KEY `sequence` (`sequence`(200), `protein_sequence_id`) USING HASH
);
CREATE TABLE protein_sequence_list
(
  protein_sequence_list_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  hash                     BIGINT          NOT NULL,
  KEY `hash` (`hash`, `protein_sequence_list_id`)
);
CREATE TABLE protein_sequence_list_members
(
  protein_sequence_list_id INT NOT NULL,
  protein_sequence_id      INT NOT NULL
);
CREATE TABLE report
(
  report_id      BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  report_file    LONGTEXT,
  date_created   DATETIME,
  transaction_id INT,
  KEY `Report_transaction_index` (`transaction_id`)
);
CREATE TABLE saved_parameters
(
  saved_parameter_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name               LONGTEXT,
  owner              INT,
  parameters         INT,
  creation           INT,
  deletion           INT
);
CREATE TABLE scaffold_settings
(
  scaffold_settings_id         INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  protein_probability          DOUBLE,
  peptide_probability          DOUBLE,
  minimum_peptide_count        INT,
  minimum_non_tryptic_terminii INT,
  starred_proteins             INT,
  save_only_identified_spectra BIT,
  save_no_spectra              BIT,
  connect_to_ncbi              BIT,
  annotate_with_goa            BIT
);
CREATE TABLE search_engine
(
  search_engine_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  code             VARCHAR(15)     NOT NULL,
  creation         INT,
  deletion         INT
);
CREATE TABLE search_parameters
(
  search_parameter_id      INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  curation                 INT,
  protease                 INT,
  missed_cleavages         INT,
  fixed_mods               INT,
  variable_mods            INT,
  peptide_tolerance_value  DOUBLE,
  peptide_tolerance_unit   VARCHAR(10),
  fragment_tolerance_value DOUBLE,
  fragment_tolerance_unit  VARCHAR(10),
  instrument               INT,
  extract_msn_settings     INT,
  scaffold_settings        INT
);
CREATE TABLE search_result
(
  search_result_id           INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  protein_group_list_id      INT,
  tandem_mass_spec_sample_id INT,
  UNIQUE KEY `uniqueness` (`protein_group_list_id`, `tandem_mass_spec_sample_id`)
);
CREATE TABLE search_result_list
(
  search_result_list_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  hash                  BIGINT          NOT NULL
);
CREATE TABLE search_result_list_members
(
  search_result_list_id INT NOT NULL,
  search_result_id      INT NOT NULL
);
CREATE TABLE source_database_archive
(
  source_id            INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  source_url           VARCHAR(128),
  source_path          VARCHAR(255),
  server_creation_date DATETIME,
  download_date        DATETIME
);
CREATE TABLE spectrum_qa
(
  spectrum_qa_id  INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  engine          VARCHAR(15),
  param_file_path VARCHAR(255)
);
CREATE TABLE starred_proteins
(
  starred_proteins_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  starred             LONGTEXT,
  protein_delimiter   VARCHAR(20),
  regular_expression  BIT,
  match_name          BIT
);
CREATE TABLE swift_db_version
(
  id         INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  db_version INT             NOT NULL,
  UNIQUE KEY `db_version` (`db_version`)
);
CREATE TABLE swift_search_definition
(
  swift_search_definition_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  title                      VARCHAR(100),
  owner                      INT,
  output_folder              VARCHAR(255),
  spectrum_qa                INT,
  peptide_report             INT,
  extract_msn_settings       INT,
  scaffold_settings          INT,
  public_mgf_files           BIT,
  public_search_files        BIT,
  search_parameters          INT
);
CREATE TABLE tandem_mass_spec_sample
(
  tandem_mass_spec_sample_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  file                       VARCHAR(255)    NOT NULL,
  last_modified              DATETIME        NOT NULL,
  ms1_spectra                INT,
  ms2_spectra                INT,
  ms3_plus_spectra           INT,
  instrument_name            VARCHAR(40),
  instrument_serial_number   VARCHAR(20),
  start_time                 DATETIME,
  run_time_seconds           DOUBLE,
  comment                    LONGTEXT,
  sample_information         LONGTEXT,
  UNIQUE KEY `file` (`file`, `last_modified`),
  UNIQUE KEY `uniqueness` (`file`, `last_modified`)
);
CREATE TABLE task
(
  task_id          INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  task_name        VARCHAR(60),
  queue_timestamp  DATETIME,
  start_timestamp  DATETIME,
  end_timestamp    DATETIME,
  error_code       INT,
  error_message    LONGTEXT,
  exception        LONGTEXT,
  transaction_id   INT,
  task_state       BIGINT,
  description_long LONGTEXT,
  grid_job_id      VARCHAR(25),
  out_log          LONGTEXT,
  err_log          LONGTEXT,
  host             VARCHAR(255),
  percent_done     REAL,
  KEY `task_transaction_index` (`transaction_id`),
  KEY `task_start_timestamp_index` (`start_timestamp`)
);
CREATE TABLE task_state
(
  task_state_id BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  description   VARCHAR(60)
);
CREATE TABLE transaction
(
  transaction_id     INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  title              VARCHAR(100),
  submitting_user    INT,
  xml_def_file       LONGTEXT,
  swift_search       INT,
  start_timestamp    DATETIME,
  end_timestamp      DATETIME,
  error_code         INT,
  error_message      LONGTEXT,
  num_tasks          INT,
  tasks_with_warning INT,
  tasks_failed       INT,
  tasks_completed    INT,
  hidden             INT,
  KEY `transaction_start_timestamp_index` (`start_timestamp`),
  KEY `transaction_hidden_index` (`hidden`)
);
CREATE TABLE user_preferences
(
  user_id          INT         NOT NULL,
  preference_value LONGTEXT,
  preference_name  VARCHAR(40) NOT NULL
);
CREATE TABLE workflow_user
(
  workflow_user_id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  first_name       VARCHAR(60),
  last_name        VARCHAR(60),
  user_password    VARCHAR(30),
  user_name        VARCHAR(60),
  initials         VARCHAR(6),
  rights           BIGINT,
  creation         INT,
  deletion         INT
);
ALTER TABLE analysis ADD FOREIGN KEY (report_id) REFERENCES report (report_id);
ALTER TABLE analysis ADD FOREIGN KEY (biological_sample_list_id) REFERENCES biological_sample_list (biological_sample_list_id);
ALTER TABLE biological_sample ADD FOREIGN KEY (search_result_list_id) REFERENCES search_result_list (search_result_list_id);
ALTER TABLE biological_sample_list_members ADD FOREIGN KEY (biological_sample_id) REFERENCES biological_sample (biological_sample_id);
ALTER TABLE biological_sample_list_members ADD FOREIGN KEY (biological_sample_list_id) REFERENCES biological_sample_list (biological_sample_list_id);
ALTER TABLE curation ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE curation ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE curation_data_source ADD FOREIGN KEY (auto_transform) REFERENCES curation_header_transform (header_transform_id);
ALTER TABLE curation_data_source ADD FOREIGN KEY (auto_transform) REFERENCES curation_header_transform (header_transform_id);
ALTER TABLE curation_step_database_upload ADD FOREIGN KEY (upload_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_database_upload ADD FOREIGN KEY (upload_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_header_filter ADD FOREIGN KEY (header_filter_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_header_filter ADD FOREIGN KEY (header_filter_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_header_transform ADD FOREIGN KEY (header_transform_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_header_transform ADD FOREIGN KEY (header_transform_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_list ADD FOREIGN KEY (step_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_list ADD FOREIGN KEY (curation_id) REFERENCES curation (curation_id);
ALTER TABLE curation_step_list ADD FOREIGN KEY (step_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_make_decoy ADD FOREIGN KEY (sequence_manipulation_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_make_decoy ADD FOREIGN KEY (sequence_manipulation_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_manual_inclusion ADD FOREIGN KEY (manual_inclusion_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_manual_inclusion ADD FOREIGN KEY (manual_inclusion_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_new_db ADD FOREIGN KEY (source_db) REFERENCES source_database_archive (source_id);
ALTER TABLE curation_step_new_db ADD FOREIGN KEY (new_database_id) REFERENCES curation_step (step_id);
ALTER TABLE curation_step_new_db ADD FOREIGN KEY (new_database_id) REFERENCES curation_step (step_id);
ALTER TABLE enabled_engines_set ADD FOREIGN KEY (set_id) REFERENCES enabled_engines (enabled_engines_id);
ALTER TABLE enabled_engines_set ADD FOREIGN KEY (search_engine_id) REFERENCES search_engine (search_engine_id);
ALTER TABLE file_search ADD FOREIGN KEY (search_parameters) REFERENCES search_parameters (search_parameter_id);
ALTER TABLE file_search ADD FOREIGN KEY (enabled_engines) REFERENCES enabled_engines (enabled_engines_id);
ALTER TABLE file_search ADD FOREIGN KEY (input_files_id) REFERENCES swift_search_definition (swift_search_definition_id);
ALTER TABLE instrument ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE instrument ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE instrument_series ADD FOREIGN KEY (ion_series_id) REFERENCES ion_series (ion_series_id);
ALTER TABLE instrument_series ADD FOREIGN KEY (instrument_id) REFERENCES instrument (instrument_id);
ALTER TABLE ion_series ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE ion_series ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE `mod` ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE `mod` ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE mod_alt_names ADD FOREIGN KEY (alt_name_id) REFERENCES `mod` (mod_id);
ALTER TABLE mod_set_specificities ADD FOREIGN KEY (specificity_id) REFERENCES mod_specificity (specificity_id);
ALTER TABLE mod_set_specificities ADD FOREIGN KEY (set_id) REFERENCES mod_set (mod_set_id);
ALTER TABLE mod_specificity ADD FOREIGN KEY (mod_id) REFERENCES `mod` (mod_id);
ALTER TABLE protease ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE protease ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE protein_group ADD FOREIGN KEY (protein_sequence_list_id) REFERENCES protein_sequence_list (protein_sequence_list_id);
ALTER TABLE protein_group_list_members ADD FOREIGN KEY (protein_group_id) REFERENCES protein_group (protein_group_id);
ALTER TABLE protein_group_list_members ADD FOREIGN KEY (protein_group_list_id) REFERENCES protein_group_list (protein_group_list_id);
ALTER TABLE protein_sequence_list_members ADD FOREIGN KEY (protein_sequence_id) REFERENCES protein_sequence (protein_sequence_id);
ALTER TABLE protein_sequence_list_members ADD FOREIGN KEY (protein_sequence_list_id) REFERENCES protein_sequence_list (protein_sequence_list_id);
ALTER TABLE report ADD FOREIGN KEY (transaction_id) REFERENCES transaction (transaction_id);
ALTER TABLE report ADD FOREIGN KEY (transaction_id) REFERENCES transaction (transaction_id);
ALTER TABLE saved_parameters ADD FOREIGN KEY (owner) REFERENCES workflow_user (workflow_user_id);
ALTER TABLE saved_parameters ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE saved_parameters ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE saved_parameters ADD FOREIGN KEY (parameters) REFERENCES search_parameters (search_parameter_id);
ALTER TABLE scaffold_settings ADD FOREIGN KEY (starred_proteins) REFERENCES starred_proteins (starred_proteins_id);
ALTER TABLE scaffold_settings ADD FOREIGN KEY (starred_proteins) REFERENCES starred_proteins (starred_proteins_id);
ALTER TABLE search_engine ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE search_engine ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (variable_mods) REFERENCES mod_set (mod_set_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (scaffold_settings) REFERENCES scaffold_settings (scaffold_settings_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (fixed_mods) REFERENCES mod_set (mod_set_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (protease) REFERENCES protease (protease_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (instrument) REFERENCES instrument (instrument_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (curation) REFERENCES curation (curation_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (extract_msn_settings) REFERENCES extract_msn_settings (extract_msn_settings_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (curation) REFERENCES curation (curation_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (extract_msn_settings) REFERENCES extract_msn_settings (extract_msn_settings_id);
ALTER TABLE search_parameters ADD FOREIGN KEY (scaffold_settings) REFERENCES scaffold_settings (scaffold_settings_id);
ALTER TABLE search_result ADD FOREIGN KEY (protein_group_list_id) REFERENCES protein_group_list (protein_group_list_id);
ALTER TABLE search_result ADD FOREIGN KEY (tandem_mass_spec_sample_id) REFERENCES tandem_mass_spec_sample (tandem_mass_spec_sample_id);
ALTER TABLE search_result_list_members ADD FOREIGN KEY (search_result_list_id) REFERENCES search_result_list (search_result_list_id);
ALTER TABLE search_result_list_members ADD FOREIGN KEY (search_result_id) REFERENCES search_result (search_result_id);
ALTER TABLE swift_search_definition ADD FOREIGN KEY (peptide_report) REFERENCES peptide_report (peptide_report_id);
ALTER TABLE swift_search_definition ADD FOREIGN KEY (search_parameters) REFERENCES search_parameters (search_parameter_id);
ALTER TABLE swift_search_definition ADD FOREIGN KEY (spectrum_qa) REFERENCES spectrum_qa (spectrum_qa_id);
ALTER TABLE swift_search_definition ADD FOREIGN KEY (owner) REFERENCES workflow_user (workflow_user_id);
ALTER TABLE task ADD FOREIGN KEY (transaction_id) REFERENCES transaction (transaction_id);
ALTER TABLE task ADD FOREIGN KEY (task_state) REFERENCES task_state (task_state_id);
ALTER TABLE task ADD FOREIGN KEY (transaction_id) REFERENCES transaction (transaction_id);
ALTER TABLE transaction ADD FOREIGN KEY (submitting_user) REFERENCES workflow_user (workflow_user_id);
ALTER TABLE transaction ADD FOREIGN KEY (swift_search) REFERENCES swift_search_definition (swift_search_definition_id);
ALTER TABLE user_preferences ADD FOREIGN KEY (user_id) REFERENCES workflow_user (workflow_user_id);
ALTER TABLE workflow_user ADD FOREIGN KEY (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE workflow_user ADD FOREIGN KEY (creation) REFERENCES change_audit (change_audit_id);

-- Initial database version is to be instantly updated by the DbMigrator script
INSERT INTO swift_db_version (id, db_version) VALUES (1, 1);

-- @UNDO
SET foreign_key_checks = 0;
DROP TABLE analysis;
DROP TABLE biological_sample;
DROP TABLE biological_sample_list;
DROP TABLE biological_sample_list_members;
DROP TABLE change_audit;
DROP TABLE curation;
DROP TABLE curation_data_source;
DROP TABLE curation_header_transform;
DROP TABLE curation_step;
DROP TABLE curation_step_database_upload;
DROP TABLE curation_step_header_filter;
DROP TABLE curation_step_header_transform;
DROP TABLE curation_step_list;
DROP TABLE curation_step_make_decoy;
DROP TABLE curation_step_manual_inclusion;
DROP TABLE curation_step_new_db;
DROP TABLE enabled_engines;
DROP TABLE enabled_engines_set;
DROP TABLE extract_msn_settings;
DROP TABLE file_search;
DROP TABLE instrument;
DROP TABLE instrument_series;
DROP TABLE ion_series;
DROP TABLE `mod`;
DROP TABLE mod_alt_names;
DROP TABLE mod_set;
DROP TABLE mod_set_specificities;
DROP TABLE mod_specificity;
DROP TABLE peptide_report;
DROP TABLE protease;
DROP TABLE protein_database_entry;
DROP TABLE protein_group;
DROP TABLE protein_group_list;
DROP TABLE protein_group_list_members;
DROP TABLE protein_sequence;
DROP TABLE protein_sequence_list;
DROP TABLE protein_sequence_list_members;
DROP TABLE report;
DROP TABLE saved_parameters;
DROP TABLE scaffold_settings;
DROP TABLE search_engine;
DROP TABLE search_parameters;
DROP TABLE search_result;
DROP TABLE search_result_list;
DROP TABLE search_result_list_members;
DROP TABLE source_database_archive;
DROP TABLE spectrum_qa;
DROP TABLE starred_proteins;
DROP TABLE swift_db_version;
DROP TABLE swift_search_definition;
DROP TABLE tandem_mass_spec_sample;
DROP TABLE task;
DROP TABLE task_state;
DROP TABLE transaction;
DROP TABLE user_preferences;
DROP TABLE workflow_user;
SET foreign_key_checks = 1;
