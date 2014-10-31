ALTER TABLE curation ADD COLUMN steps_json LONGTEXT;

# You need to execute the createDocStore_curator_steps.rb script on the database

-- @UNDO
