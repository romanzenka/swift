ALTER TABLE `tandem_mass_spec_sample`
CHANGE COLUMN `instrument_serial_number`
`instrument_serial_number` VARCHAR(60)
  NULL DEFAULT NULL;

-- @UNDO

ALTER TABLE `tandem_mass_spec_sample`
CHANGE COLUMN `instrument_serial_number`
`instrument_serial_number` VARCHAR(20)
  NULL DEFAULT NULL;


