-- Add analysis_id column to report table:

ALTER TABLE report ADD COLUMN `analysis_id` INT(11) NULL DEFAULT NULL
AFTER `transaction_id`;

ALTER TABLE report
ADD KEY report_idx_2 (analysis_id),
ADD FOREIGN KEY report_ibfk_2 (analysis_id) REFERENCES analysis (analysis_id);

UPDATE report AS r LEFT JOIN analysis AS a
    ON a.report_id = r.report_id
SET
  r.analysis_id = a.analysis_id;


-- @UNDO

ALTER TABLE report
DROP COLUMN `analysis_id`;