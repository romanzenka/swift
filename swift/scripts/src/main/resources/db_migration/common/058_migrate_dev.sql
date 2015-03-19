# Redo all the output_folder paths
UPDATE atlas_dev.swift_search_definition
SET output_folder = REPLACE(output_folder, 'shared:/', 'shared:/prod/')
WHERE output_folder NOT LIKE 'shared:/prod/%';
UPDATE atlas_dev.swift_search_definition
SET output_folder = REPLACE(output_folder, 'shared:/prod/../raid1/projects/amyloid_dogan_2007/', 'shared:/prod/');
UPDATE atlas_dev.swift_search_definition
SET output_folder = REPLACE(output_folder, 'local:/mnt/raid1/projects/amyloid_dogan_2007/', 'shared:/prod/');

# Redo paths to databases (they are now in their own databases folder on /odin/dev
UPDATE atlas_dev.curation
SET curated_filename = REPLACE(curated_filename, 'shared:/databases/', 'shared:/dev/databases/');
UPDATE atlas_dev.curation
SET curated_filename = REPLACE(curated_filename, 'shared:/swift/dev/var/database/', 'shared:/dev/databases/');
UPDATE atlas_dev.curation
SET curated_filename = REPLACE(curated_filename, 'shared:/swift/prod/', 'shared:/dev/databases/');

# Curator steps
UPDATE atlas_dev.curation
SET steps_json = replace(steps_json, 'shared:/swift/prod/var/dbcurator/',
                         'shared:/dev/apps/swift/lokidev/var/dbcurator/');

# File_search
UPDATE atlas_dev.file_search
SET input_file = REPLACE(input_file, 'shared:/../raid1/projects/amyloid_dogan_2007/', 'shared:/prod/');
UPDATE atlas_dev.file_search
SET input_file = REPLACE(input_file, 'local:/mnt/raid1/projects/amyloid_dogan_2007/', 'shared:/prod/');
UPDATE atlas_dev.file_search
SET input_file = REPLACE(input_file, 'shared:/', 'shared:/prod/')
WHERE input_file NOT LIKE 'shared:/prod/%';


# log
UPDATE atlas_dev.log
SET out_log = replace(out_log, 'shared:/swift/dev/var/log/', 'shared:/dev/apps/swift/lokidev/var/log/'),
  err_log   = replace(err_log, 'shared:/swift/dev/var/log/', 'shared:/dev/apps/swift/lokidev/var/log/');

# Report
UPDATE atlas_dev.report
SET report_file = REPLACE(report_file, 'shared:/', 'shared:/prod/')
WHERE report_file NOT LIKE 'shared:/prod/%';


# Source_database_archive
UPDATE atlas_dev.source_database_archive
SET source_path = REPLACE(source_path, 'shared:/swift/prod/var/dbcurator/archive/',
                          'shared:/dev/apps/swift/lokidev/var/dbcurator/archive/');
UPDATE atlas_dev.source_database_archive
SET source_path = REPLACE(source_path, 'shared:/swift/dev/var/dbcurator/archive/',
                          'shared:/dev/apps/swift/lokidev/var/dbcurator/archive/');

# Tandem mass spec sample
UPDATE atlas_dev.tandem_mass_spec_sample
SET sample_file = REPLACE(sample_file, 'shared:/prod/', 'shared:/prod_old/')
WHERE sample_file LIKE 'shared:/prod/%';
UPDATE atlas_dev.tandem_mass_spec_sample
SET sample_file = REPLACE(sample_file, 'shared:/../raid1/projects/amyloid_dogan_2007/', 'shared:/prod/')
WHERE sample_file LIKE 'shared:/../raid1/projects/amyloid_dogan_2007/%';
UPDATE atlas_dev.tandem_mass_spec_sample
SET sample_file = REPLACE(sample_file, 'local:/mnt/raid1/projects/amyloid_dogan_2007/', 'shared:/prod/')
WHERE sample_file LIKE 'local:/mnt/raid1/projects/amyloid_dogan_2007/%';
UPDATE atlas_dev.tandem_mass_spec_sample
SET sample_file = REPLACE(sample_file, 'shared:/', 'shared:/prod/')
WHERE sample_file NOT LIKE 'shared:/prod/%' AND sample_file NOT LIKE 'shared:/prod_old/%';


# Task
UPDATE atlas_dev.task
SET description_long = REPLACE(description_long, 'shared:/swift/dev/var/cache/',
                               'shared:/dev/apps/swift/lokidev/var/cache')
WHERE description_long LIKE '%shared:/swift/dev/var/cache/%';
UPDATE atlas_dev.task
SET description_long = REPLACE(description_long, 'shared:/Clinical/', 'shared:/prod/Clinical')
WHERE description_long LIKE '%shared:/Clinical/%';
UPDATE atlas_dev.task
SET
  description_long = REPLACE(description_long, 'shared:/ResearchandDevelopment/', 'shared:/prod/ResearchandDevelopment')
WHERE description_long LIKE '%shared:/ResearchandDevelopment/%';
UPDATE atlas_dev.task
SET description_long = REPLACE(description_long, 'shared:/test/', 'shared:/prod/test')
WHERE description_long LIKE '%shared:/test/%';
UPDATE atlas_dev.task
SET description_long = REPLACE(description_long, 'shared:/Training/', 'shared:/prod/Training')
WHERE description_long LIKE '%shared:/Training/%';
UPDATE atlas_dev.task
SET description_long = REPLACE(description_long, 'shared:/projects/amyloid_dogan_2007/', 'shared:/prod/')
WHERE description_long LIKE '%shared:/projects/amyloid_dogan_2007/%';
UPDATE atlas_dev.task
SET description_long = REPLACE(description_long, 'shared:/atlas/amyloid/', 'shared:/prod/')
WHERE description_long LIKE '%shared:/atlas/amyloid/%';



