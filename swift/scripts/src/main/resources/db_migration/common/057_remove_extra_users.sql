# MSSQL SPECIFIC!!!

INSERT INTO change_audit (reason, change_date) VALUES ('Remove obsolete users', getdate());

DECLARE @delete_id INT;
SELECT @delete_id = MAX(change_audit_id)
FROM change_audit;
UPDATE workflow_user
SET deletion = @delete_id
WHERE
  last_name = 'Project' AND first_name IN ('Erikson', 'Feldman', 'LC Clone', 'Miller', 'Zhang') AND deletion IS NULL;
UPDATE workflow_user
SET first_name = 'Software'
WHERE last_name = 'Test' AND first_name = 'Mprc';

-- @UNDO