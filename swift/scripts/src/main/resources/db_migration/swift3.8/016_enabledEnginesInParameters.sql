-- enabled_engines used to be a field at file_search
-- now it is a part of search_parameters
-- * search_parameters needs to be cloned for each distinct value of enabled_engines and pointed to enabled_engines
-- * saved parameters needs to point to the most commonly used search parameters

-- Step 1:
--  Create a table that shows
-- * saved parameters
-- * enabled engines
-- * count (how many times did this combo occur)
--
-- This is all determined using the file_search table that references both enabled engines and saved params

ALTER TABLE search_parameters
ADD COLUMN enabled_engines INT(11) NULL DEFAULT NULL
AFTER scaffold_settings,
ADD FOREIGN KEY search_parameters_ibfk_9 (enabled_engines) REFERENCES enabled_engines (enabled_engines_id);

CREATE TABLE `temp_saved_enabled_counts` (
  `temp_id`                  INT NOT NULL AUTO_INCREMENT,
  `search_parameters_id`     INT NULL,
  `enabled_engines_id`       INT NULL,
  `count`                    INT NULL,
  `saved_candidate`          BIT,
  `new_search_parameters_id` INT NULL,
  PRIMARY KEY (`temp_id`));

ALTER TABLE temp_saved_enabled_counts ADD FOREIGN KEY temp_saved_enabled_counts_ibfk_1 (search_parameters_id) REFERENCES search_parameters (search_parameter_id);
ALTER TABLE temp_saved_enabled_counts ADD FOREIGN KEY temp_saved_enabled_counts_ibfk_2 (enabled_engines_id) REFERENCES enabled_engines (enabled_engines_id);

-- Fill the table with data
INSERT INTO temp_saved_enabled_counts (search_parameters_id, enabled_engines_id, count, saved_candidate)
  (SELECT
     s.search_parameters,
     f.enabled_engines,
     count(*),
     0
   FROM file_search f, swift_search_definition s
   WHERE s.swift_search_definition_id = f.swift_search_definition_id
   GROUP BY s.search_parameters, f.enabled_engines
  )
  UNION
  (
    SELECT
      f.search_parameters,
      f.enabled_engines,
      COUNT(*),
      0
    FROM file_search f
    GROUP BY f.search_parameters, f.enabled_engines
    HAVING f.search_parameters IS NOT NULL
  );

-- Mark the candidates for saving
-- We group by search_parameters_id and pick the ones with most files
-- If there is a tie, we pick the one which comes first in the table (minimum temp_id).
UPDATE temp_saved_enabled_counts AS u,
  (SELECT
     min(t.temp_id) AS mint
   FROM temp_saved_enabled_counts AS t
     INNER JOIN
     (SELECT
        search_parameters_id,
        max(count) AS m
      FROM temp_saved_enabled_counts
      GROUP BY search_parameters_id) AS i
       ON t.search_parameters_id = i.search_parameters_id
          AND t.count = i.m
   GROUP BY t.search_parameters_id) AS tids
SET u.saved_candidate = 1
WHERE u.temp_id = tids.mint;

UPDATE temp_saved_enabled_counts
SET new_search_parameters_id = search_parameters_id
WHERE saved_candidate = 1;

SELECT
  @max_search_parameters := max(search_parameter_id) + 1
FROM search_parameters;

UPDATE temp_saved_enabled_counts
SET new_search_parameters_id = @max_search_parameters := @max_search_parameters + 1
WHERE
  saved_candidate = 0
ORDER BY temp_id;


INSERT INTO search_parameters (
  search_parameter_id,
  curation,
  protease,
  missed_cleavages,
  min_termini_cleavages,
  fixed_mods,
  variable_mods,
  peptide_tolerance_value,
  peptide_tolerance_unit,
  fragment_tolerance_value,
  fragment_tolerance_unit,
  instrument,
  extract_msn_settings,
  scaffold_settings,
  enabled_engines)
  SELECT
    c.new_search_parameters_id,
    curation,
    protease,
    missed_cleavages,
    min_termini_cleavages,
    fixed_mods,
    variable_mods,
    peptide_tolerance_value,
    peptide_tolerance_unit,
    fragment_tolerance_value,
    fragment_tolerance_unit,
    instrument,
    extract_msn_settings,
    scaffold_settings,
    c.enabled_engines_id
  FROM search_parameters AS p,
    temp_saved_enabled_counts AS c
  WHERE p.search_parameter_id = c.search_parameters_id
        AND c.saved_candidate = 0;

UPDATE search_parameters AS p,
  temp_saved_enabled_counts AS c
SET p.enabled_engines = c.enabled_engines_id
WHERE p.search_parameter_id = c.new_search_parameters_id;

-- There are still some search_parameter sets that have enabled_engines as null
-- Those are the instances where a parameter set was saved, but no searches ran against it
-- Select the enabled engines that appear the most times and then replace all the nulls with the
-- most popular choice

SELECT
  @most_common_enabled_engines := max(enabled_engines)
FROM search_parameters
GROUP BY enabled_engines
HAVING enabled_engines IS NOT NULL
ORDER BY count(enabled_engines) DESC
LIMIT 1;

UPDATE search_parameters
SET enabled_engines = @most_common_enabled_engines
WHERE enabled_engines IS NULL;

-- Renumber the file_search table direct parameter names
UPDATE
    file_search AS f,
    temp_saved_enabled_counts AS t
SET f.search_parameters = t.new_search_parameters_id
WHERE f.search_parameters = t.search_parameters_id
      AND f.enabled_engines = t.enabled_engines_id;

-- -----------------------------------------------------------------------------------
-- Now we need to split the swift_search_definition table too
CREATE TABLE temp_search_definition (
  `temp_search_definition_id`      INT NOT NULL AUTO_INCREMENT,
  `swift_search_definition_id`     INT NULL,
  `enabled_engines_id`             INT NULL,
  `new_swift_search_definition_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`temp_search_definition_id`));

ALTER TABLE temp_search_definition
ADD FOREIGN KEY temp_search_definition_ibfk_1_idx (swift_search_definition_id)
REFERENCES swift_search_definition (swift_search_definition_id),
ADD FOREIGN KEY temp_search_definition_ibfk_2_idx (enabled_engines_id)
REFERENCES enabled_engines (enabled_engines_id);

-- The saved ones get copied
INSERT INTO temp_search_definition (swift_search_definition_id, enabled_engines_id, new_swift_search_definition_id)
  SELECT
    f.swift_search_definition_id,
    f.enabled_engines,
    f.swift_search_definition_id
  FROM file_search AS f,
    temp_saved_enabled_counts AS t,
    swift_search_definition AS d
  WHERE f.swift_search_definition_id = d.swift_search_definition_id
        AND d.search_parameters = t.search_parameters_id
        AND t.enabled_engines_id = f.enabled_engines
        AND t.saved_candidate = 1;

SELECT
  @new_def_id := max(swift_search_definition_id)
FROM swift_search_definition;

-- Where we have a new parameter set, create a new id
INSERT INTO temp_search_definition (swift_search_definition_id, enabled_engines_id, new_swift_search_definition_id)
  SELECT
    f.swift_search_definition_id,
    f.enabled_engines,
    @new_def_id := @new_def_id + 1
  FROM file_search AS f,
    temp_saved_enabled_counts AS t,
    swift_search_definition AS d
  WHERE f.swift_search_definition_id = d.swift_search_definition_id
        AND d.search_parameters = t.search_parameters_id
        AND t.enabled_engines_id = f.enabled_engines
        AND t.saved_candidate = 0;

-- clone the search definitions where enabled_engines is not the most prevalent one
INSERT INTO swift_search_definition (
  swift_search_definition_id,
  title,
  owner,
  output_folder,
  spectrum_qa,
  peptide_report,
  extract_msn_settings,
  scaffold_settings,
  public_mgf_files,
  public_mzxml_files,
  public_search_files,
  search_parameters)
  SELECT
    t.new_swift_search_definition_id,
    s.title,
    s.owner,
    s.output_folder,
    s.spectrum_qa,
    s.peptide_report,
    s.extract_msn_settings,
    s.scaffold_settings,
    s.public_mgf_files,
    s.public_mzxml_files,
    s.public_search_files,
    p.new_search_parameters_id
  FROM
    temp_search_definition AS t,
    swift_search_definition AS s,
    temp_saved_enabled_counts AS p
  WHERE
    t.swift_search_definition_id = s.swift_search_definition_id
    AND t.enabled_engines_id = p.enabled_engines_id
    AND p.search_parameters_id = s.search_parameters
    AND t.swift_search_definition_id != t.new_swift_search_definition_id;
-- to be newly created, the new id must differ

-- Redirect all the transaction search parameters to the newly created ones
UPDATE
    transaction AS t,
    swift_search_definition AS d,
    file_search AS f,
    temp_search_definition AS td
SET
  t.swift_search = td.new_swift_search_definition_id
WHERE
  f.swift_search_definition_id = d.swift_search_definition_id
  AND t.swift_search = d.swift_search_definition_id
  AND td.enabled_engines_id = f.enabled_engines
  AND td.swift_search_definition_id = d.swift_search_definition_id;

-- Redirect all the file_search to the new swift search definitions
UPDATE
    file_search AS f,
    temp_search_definition AS td
SET
  f.swift_search_definition_id = td.new_swift_search_definition_id
WHERE
  f.swift_search_definition_id = td.swift_search_definition_id
  AND f.enabled_engines = td.enabled_engines_id;

-- Drop link from file_search to enabled_engines

ALTER TABLE file_search
DROP FOREIGN KEY file_search_ibfk_2;
ALTER TABLE file_search
DROP COLUMN enabled_engines,
DROP INDEX enabled_engines,
DROP INDEX input_file,
ADD UNIQUE INDEX input_file
  (input_file ASC,
   biological_sample ASC,
   category_name ASC,
   experiment ASC,
   search_parameters ASC,
   swift_search_definition_id ASC);

-- And drop the temp tables
DROP TABLE temp_saved_enabled_counts;
DROP TABLE temp_search_definition;

-- @UNDO

-- No way I am writing this