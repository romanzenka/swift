ALTER TABLE `search_result_list_members`
        ADD PRIMARY KEY (`search_result_list_id`, `search_result_id`);

-- @UNDO

ALTER TABLE `search_result_list_members`
        DROP PRIMARY KEY;
