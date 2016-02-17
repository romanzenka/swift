INSERT INTO change_audit (reason, change_date) VALUES ('Disabling old users after migrating Atlas to Loki', now());

SET @userAuditId = LAST_INSERT_ID();

UPDATE workflow_user
SET deletion = @userAuditId
WHERE user_name IN (
  'theis.jason+erikson@mayo.edu',
  'theis.jason+feldman@mayo.edu',
  'theis.jason1@mayo.edu',
  'theis.jason+miller@mayo.edu',
  'mprctest@localhost',
  'theis.jason+zhang@mayo.edu'
);

# No new users are added

##############################

INSERT INTO change_audit (reason, change_date) VALUES ('Updating databases after migrating Atlas to Loki', now());
SET @dbAuditId = LAST_INSERT_ID();

UPDATE curation
SET deletion = @dbAuditId
WHERE short_name IN (
  'Alt.Protein_V1.0',
  'FuzeonTestV1',
  'IGMT2.1-APDBv1.1',
  'Jelinek_IGMT_APDB',
  'Ramirez_V1',
  'SPhu_2012_06',
  'SPhu2011ver12_Iso'
);

INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('APDBv2-IGVARv4', '2015-07-07 15:41:25', 'shared:/databases/APDBv2-IGVARv4.fasta',
   'Clinical SwissProt (2014_07) + APDBv2 + ALBase + MayoIGV', 'theis.jason@mayo.edu', '2015-07-07 15:41:25',
   '2015-07-07 15:41:25',
   'Clinical SwissProt (2014_07) + Revised Contaminants + Insulins + Fuzeon + ALBase + Mayo IGV + APDBv2', 'Rev_',
   @dbAuditId, NULL, '{
  "version": 1,
  "steps": [
    {
      "pathToUploadedFile": "shared:/apps/swift/loki/var/dbcurator/uploads/2015-07-07_15-41-14_APDBv2.0_IGVARv4.fasta",
      "fileName": "U:\\databases\\APDBv2.0_IGVARv4.fasta",
      "lastRunCompletionCount": 25025,
      "step_type": "database_upload"
    },
    {
      "overwriteMode": false,
      "manipulatorType": 1,
      "lastRunCompletionCount": 50050,
      "step_type": "make_decoy"
    }
  ]
}');
INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('APDBv2.1-IGVARv4', '2015-07-20 19:04:34', 'shared:/databases/APDBv2.1-IGVARv4.fasta',
   'Clinical SwissProt (2014_07) + APDBv2 + ALBase + MayoIGV', 'theis.jason@mayo.edu', '2015-07-20 19:04:34',
   '2015-07-20 19:04:34',
   'Clinical SwissProt (2014_07) + Revised Contaminants + Insulins + Fuzeon + ALBase + Mayo IGV + APDBv2.1', 'Rev_',
   @dbAuditId, NULL, '{
  "version": 1,
  "steps": [
    {
      "pathToUploadedFile": "shared:/apps/swift/loki/var/dbcurator/uploads/2015-07-20_19-02-26_APDBv2.1_IGVARv4.fasta",
      "fileName": "O:\\databases\\APDBv2.1_IGVARv4_20150719\\APDBv2.1_IGVARv4.fasta",
      "lastRunCompletionCount": 25025,
      "step_type": "database_upload"
    },
    {
      "overwriteMode": false,
      "manipulatorType": 1,
      "lastRunCompletionCount": 50050,
      "step_type": "make_decoy"
    }
  ]
}');
INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('APDBv2.2-IGVARv4', '2015-07-23 17:44:59', 'shared:/databases/APDBv2.2-IGVARv4.fasta',
   'Clinical SwissProt (2014_07) + APDBv2 + ALBase + MayoIGV', 'theis.jason@mayo.edu', '2015-07-23 17:44:59',
   '2015-07-23 17:44:59',
   'Clinical SwissProt (2014_07) + Revised Contaminants + Insulins + Fuzeon + ALBase + Mayo IGV + APDBv2.2', 'Rev_',
   @dbAuditId, NULL, '{
  "version": 1,
  "steps": [
    {
      "pathToUploadedFile": "shared:/apps/swift/loki/var/dbcurator/uploads/2015-07-23_17-44-33_APDBv2.2_IGVARv4.fasta",
      "fileName": "O:\\databases\\APDBv2.2_IGVARv4_20150723\\APDBv2.2_IGVARv4.fasta",
      "lastRunCompletionCount": 25025,
      "step_type": "database_upload"
    },
    {
      "overwriteMode": false,
      "manipulatorType": 1,
      "lastRunCompletionCount": 50050,
      "step_type": "make_decoy"
    }
  ]
}');
INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('APDBv2.3-IGVARv4', '2015-08-26 17:33:26', 'shared:/databases/APDBv2.3-IGVARv4.fasta',
   'Clinical SwissProt (2014_07) + APDBv2 + ALBase + MayoIGV', 'theis.jason@mayo.edu', '2015-08-26 17:33:26',
   '2015-08-26 17:33:26',
   'Clinical SwissProt (2014_07) + Revised Contaminants + Insulins + Fuzeon + ALBase + Mayo IGV + APDBv2.3', 'Rev_',
   @dbAuditId, NULL, '{
  "version": 1,
  "steps": [
    {
      "pathToUploadedFile": "shared:/apps/swift/loki/var/dbcurator/uploads/2015-08-26_17-33-13_APDBv2.3_IGVARv4.fasta",
      "fileName": "O:\\databases\\APDBv2.3_IGVARv4_20150826\\APDBv2.3_IGVARv4.fasta",
      "lastRunCompletionCount": 25026,
      "step_type": "database_upload"
    },
    {
      "overwriteMode": false,
      "manipulatorType": 1,
      "lastRunCompletionCount": 50052,
      "step_type": "make_decoy"
    }
  ]
}');
INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('APDBv2.3-IGVARv41', '2015-11-13 13:29:16', 'shared:/databases/APDBv2.3-IGVARv41.fasta',
   'Clinical SwissProt (2014_07) + APDBv2 + ALBase + MayoIGV+HIG', 'theis.jason@mayo.edu', '2015-11-13 13:29:16',
   '2015-11-13 13:29:16',
   'Clinical SwissProt (2014_07) + Revised Contaminants + Insulins + Fuzeon + ALBase + Mayo IGV + HIG+APDBv2.3', 'Rev_',
   @dbAuditId, NULL, '{
  "version": 1,
  "steps": [
    {
      "pathToUploadedFile": "shared:/apps/swift/loki/var/dbcurator/uploads/2015-11-13_13-29-06_APDBv2.3_IGVARv4.1_.fasta",
      "fileName": "U:\\databases\\APDBv2.3_IGVARv4.1_.fasta",
      "lastRunCompletionCount": 25028,
      "step_type": "database_upload"
    },
    {
      "overwriteMode": false,
      "manipulatorType": 1,
      "lastRunCompletionCount": 50056,
      "step_type": "make_decoy"
    }
  ]
}');
INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('UPS1_UPS2_2015_03', '2015-03-18 12:40:47', 'shared:/databases/UPS1_UPS2_2015_03.fasta',
   'UPS1 and UPS2 database version 03/18/2015', 'theis.jason@mayo.edu', '2015-03-18 12:40:47', '2015-03-18 12:40:47',
   '', 'Reversed_', @dbAuditId, NULL, '{
  "version": 1,
  "steps": [
    {
      "pathToUploadedFile": "shared:/apps/swift/loki/var/dbcurator/uploads/2015-03-18_12-40-07_UPS_2015_07_no_rev.fasta",
      "fileName": "F:\\databases\\UPS_2015_07_no_rev.fasta",
      "lastRunCompletionCount": 20275,
      "step_type": "database_upload"
    },
    {
      "overwriteMode": false,
      "manipulatorType": 1,
      "lastRunCompletionCount": 40550,
      "step_type": "make_decoy"
    }
  ]
}');
INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('IGVARv3-APDBv2', '2014-09-11 12:13:23', 'shared:/databases/IGVARv3-APDBv2.fasta',
   'Clinical SwissProt (2014_07) + ALBase + MayoIGV + APDBv2', 'theis.jason@mayo.edu', '2014-09-11 12:13:23',
   '2014-09-11 12:13:23',
   'Clinical SwissProt (2014_07) + Revised Contaminants + Insulins + Fuzeon + ALBase + Mayo IGV + APDBv2', 'Rev_',
   @dbAuditId, NULL,
   '{"version":1,"steps":[{"lastRunCompletionCount":25105,"step_type":"new_db","url":"file:///mnt/mprc/projects/amyloid_dogan_2007/databases/IGVARv3-APDBv2.no_rev_20140903.fasta","sort_order":0,"step":192347},{"lastRunCompletionCount":50210,"overwriteMode":"\\u0000","step_type":"make_decoy","manipulatorType":"1","sort_order":1,"step":192348}]}');
INSERT INTO curation (short_name, first_run_date, curated_filename, title, email, run_date, deployment_date, notes, decoy_regex, creation, deletion, steps_json)
VALUES
  ('SPhu_2014_07', '2014-09-10 12:30:08', 'shared:/databases/SPhu_2014_07.fasta',
   'SwissProt 2014_07 Human -Igll1 -Igll5+ RevisedContam + Reversed + Drugs', 'theis.jason@mayo.edu',
   '2014-09-10 12:30:08', '2014-09-10 12:30:08', 'SwissProt Human (2014_07) + Revised Contaminants + Insulins + Fuzeon',
   'Rev_', @dbAuditId, NULL,
   '{"version":1,"steps":[{"lastRunCompletionCount":20225,"step_type":"new_db","url":"file:///mnt/mprc/projects/amyloid_dogan_2007/databases/SPhu_2014_07_no_rev.fasta","sort_order":0,"step":192345},{"lastRunCompletionCount":40450,"overwriteMode":"\\u0000","step_type":"make_decoy","manipulatorType":"1","sort_order":1,"step":192346}]}');


-- @UNDO
