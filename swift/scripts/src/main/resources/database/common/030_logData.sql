CREATE TABLE log_data
(
  task_id INT NOT NULL,
  sort_order  INT NOT NULL,

  <key column="task_id" not-null="true"
              foreign-key="log_data_ibfk_2"/>
          <list-index column="sort_order" />
          <many-to-many column="log_data_id" class="LogData"
              foreign-key="log_data_ibfk_1"/>
);
