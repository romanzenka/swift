UPDATE swift_db_version
SET db_version={version} WHERE id = 1;
COMMIT;

-- Now on version: {version}, file: {scriptFile}

START TRANSACTION;
