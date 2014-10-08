ALTER TABLE `tandem_mass_spec_sample`
  DROP INDEX `uniqueness` ,
  ADD UNIQUE INDEX `sample_file` (`sample_file` ASC, `last_modified` ASC);

-- @UNDO

# TBD