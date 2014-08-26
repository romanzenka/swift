CREATE TABLE `quameter_pg` (
  `quameter_pg_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(40) NULL,
  `regex` TEXT NULL,
  creation      INT,
  deletion      INT,
  PRIMARY KEY (`quameter_pg_id`));

ALTER TABLE quameter_pg ADD FOREIGN KEY quameter_pg_ibfk_1 (deletion) REFERENCES change_audit (change_audit_id);
ALTER TABLE quameter_pg ADD FOREIGN KEY quameter_pg_ibfk_2 (creation) REFERENCES change_audit (change_audit_id);
ALTER TABLE quameter_pg ADD KEY quameter_pg_idx_1 (deletion);
ALTER TABLE quameter_pg ADD KEY quameter_pg_idx_2 (creation);


-- @UNDO

DROP TABLE `quameter_pg`;