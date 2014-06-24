CREATE INDEX `hash` ON biological_sample_list (hash, biological_sample_list_id);

-- @UNDO

ALTER TABLE biological_sample_list DROP INDEX `hash`;


