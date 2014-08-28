ALTER TABLE tandem_mass_spec_sample
        DROP KEY uniqueness;

ALTER TABLE tandem_mass_spec_sample
  CHANGE COLUMN `file` sample_file VARCHAR(255) NOT NULL;

ALTER TABLE tandem_mass_spec_sample
  ADD UNIQUE KEY `uniqueness` (`sample_file`, `last_modified`);

CREATE OR REPLACE VIEW analysis_to_proteins AS
select
  t.title,
  re.transaction_id,
  a.analysis_id,
  bs.biological_sample_id,
  bs.category,
  bs.sample_name,
  ts.tandem_mass_spec_sample_id,
  ts.sample_file,     -- file -> sample_file
  sr.protein_group_list_id
from analysis as a,
  biological_sample_list_members bslm,
  biological_sample as bs,
  search_result_list_members as srlm,
  search_result as sr,
  tandem_mass_spec_sample as ts,
  report as re,
  latest_functional_transaction as t
where
  a.biological_sample_list_id = bslm.biological_sample_list_id
and bslm.biological_sample_id = bs.biological_sample_id
and bs.search_result_list_id = srlm.search_result_list_id
and srlm.search_result_id = sr.search_result_id
and sr.tandem_mass_spec_sample_id = ts.tandem_mass_spec_sample_id
and re.analysis_id = a.analysis_id
and t.transaction_id = re.transaction_id;

-- @UNDO

ALTER TABLE tandem_mass_spec_sample
        DROP KEY uniqueness;

ALTER TABLE tandem_mass_spec_sample
CHANGE COLUMN sample_file `file` VARCHAR(255) NOT NULL;

ALTER TABLE tandem_mass_spec_sample
  ADD UNIQUE KEY `uniqueness` (`file`, `last_modified`);

CREATE OR REPLACE VIEW analysis_to_proteins AS
select
  t.title,
  re.transaction_id,
  a.analysis_id,
  bs.biological_sample_id,
  bs.category,
  bs.sample_name,
  ts.tandem_mass_spec_sample_id,
  ts.file,     -- sample_file -> file
  sr.protein_group_list_id
from analysis as a,
  biological_sample_list_members bslm,
  biological_sample as bs,
  search_result_list_members as srlm,
  search_result as sr,
  tandem_mass_spec_sample as ts,
  report as re,
  latest_functional_transaction as t
where
  a.biological_sample_list_id = bslm.biological_sample_list_id
and bslm.biological_sample_id = bs.biological_sample_id
and bs.search_result_list_id = srlm.search_result_list_id
and srlm.search_result_id = sr.search_result_id
and sr.tandem_mass_spec_sample_id = ts.tandem_mass_spec_sample_id
and re.analysis_id = a.analysis_id
and t.transaction_id = re.transaction_id;
