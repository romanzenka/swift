ALTER TABLE `scaffold_settings`
ADD COLUMN `use_family_protein_grouping` BIT(1) NULL
AFTER `annotate_with_goa`,
ADD COLUMN `use_independent_sample_grouping` BIT(1) NULL
AFTER `use_family_protein_grouping`;

UPDATE scaffold_settings
SET use_family_protein_grouping=1, use_independent_sample_grouping=0
WHERE 1 = 1;

-- @UNDO

ALTER TABLE `scaffold_settings`
DROP COLUMN `use_family_protein_grouping`,
DROP COLUMN `use_independent_sample_grouping`;

