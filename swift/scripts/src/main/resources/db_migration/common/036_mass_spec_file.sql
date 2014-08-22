ALTER TABLE tandem_mass_spec_sample
        DROP KEY uniqueness;

ALTER TABLE tandem_mass_spec_sample
  CHANGE COLUMN `file` sample_file VARCHAR(255) NOT NULL;

ALTER TABLE tandem_mass_spec_sample
  ADD UNIQUE KEY `uniqueness` (`sample_file`, `last_modified`);

-- @UNDO

ALTER TABLE tandem_mass_spec_sample
        DROP KEY uniqueness;

ALTER TABLE tandem_mass_spec_sample
CHANGE COLUMN sample_file `file` VARCHAR(255) NOT NULL;

ALTER TABLE tandem_mass_spec_sample
  ADD UNIQUE KEY `uniqueness` (`file`, `last_modified`);
