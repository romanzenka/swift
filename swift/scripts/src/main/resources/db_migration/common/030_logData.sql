create table log (
  log_id integer not null auto_increment,
  task_id integer,
  parent_log_id integer,
  out_log varchar(255),
  err_log varchar(255),
  primary key (log_id)
);

alter table log
add index log_data_idx_2 (parent_log_id),
add constraint log_data_ibfk_2 foreign key (parent_log_id) references log (log_id);

alter table log
add index log_data_idx_1 (task_id),
add constraint log_data_ibfk_1 foreign key (task_id) references task (task_id);

INSERT INTO log (task_id, parent_log_id, out_log, err_log)
        SELECT
          task_id,
          NULL,
          out_log,
          err_log
        FROM task
        ORDER BY task_id;

-- @UNDO

drop table log;