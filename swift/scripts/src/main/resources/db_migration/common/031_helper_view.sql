CREATE OR REPLACE VIEW latest_functional_transaction_time AS
  select tt.title as title, max(tt.start_timestamp) as newest_start_timestamp
      from `transaction` as tt
      where
        tt.hidden = false
        and tt.tasks_failed = 0
      group by tt.title;

CREATE OR REPLACE VIEW latest_functional_transaction AS
select
  t.transaction_id,
  t.title
from
  `transaction` as t,
  latest_functional_transaction_time as lft
where
  lft.title = t.title
  and lft.newest_start_timestamp = t.start_timestamp
  and t.hidden = false
  and t.tasks_failed = 0;


CREATE OR REPLACE VIEW analysis_to_proteins AS
select
  t.title,
  re.transaction_id,
  a.analysis_id,
  bs.biological_sample_id,
  bs.category,
  bs.sample_name,
  ts.tandem_mass_spec_sample_id,
  ts.file,
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

CREATE OR REPLACE VIEW protein_sequences_per_group AS
SELECT
  pglm.protein_group_list_id,
  pg.unique_peptides,
  pg.unique_spectra,
  pg.total_spectra,
  pg.percentage_total_spectra,
  pg.percentage_sequence_coverage,
  pglm.protein_group_id,
  ps.protein_sequence_id,
  ps.accession_number
from
  protein_group_list_members as pglm,
  protein_group as pg,
  protein_sequence_list_members as pslm,
  protein_sequence as ps
where
  pglm.protein_group_id = pg.protein_group_id
and pg.protein_sequence_list_id = pslm.protein_sequence_list_id
and pslm.protein_sequence_id = ps.protein_sequence_id;

CREATE OR REPLACE VIEW proteins_per_transaction AS
select
  title,
  sample_name,
  category,
  file,
  unique_peptides,
  unique_spectra,
  total_spectra,
  percentage_total_spectra,
  percentage_sequence_coverage,
  protein_group_id,
  protein_sequence_id,
  accession_number
from
  analysis_to_proteins as a,
    protein_sequences_per_group as pg
where a.protein_group_list_id=pg.protein_group_list_id;

-- @UNDO

drop view latest_functional_transaction_time;
drop view latest_functional_transaction;
drop view analysis_to_proteins;
drop view protein_sequences_per_group;