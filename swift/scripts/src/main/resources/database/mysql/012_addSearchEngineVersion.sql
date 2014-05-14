ALTER TABLE `search_engine` ADD COLUMN `version` VARCHAR(50) NOT NULL
AFTER `code`;

UPDATE search_engine
SET version='v.27'
WHERE code = 'SEQUEST';
UPDATE search_engine
SET version='2.4'
WHERE code = 'MASCOT';
UPDATE search_engine
SET version='2010.12.01.1'
WHERE code = 'TANDEM';
UPDATE search_engine
SET version=''
WHERE code = 'OMSSA';
UPDATE search_engine
SET version=''
WHERE code = 'PEAKS';
UPDATE search_engine
SET version='2.1.120'
WHERE code = 'MYRIMATCH';
UPDATE search_engine
SET version='2.6'
WHERE code = 'SCAFFOLD';
UPDATE search_engine
SET version='3.6.2', code='SCAFFOLD'
WHERE code = 'SCAFFOLD3';

-- @UNDO

UPDATE search_engine
SET code='SCAFFOLD3'
WHERE code = 'SCAFFOLD' AND version = '3.6.2';

ALTER TABLE `search_engine` DROP COLUMN `version`;

