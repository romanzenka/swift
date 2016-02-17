ALTER TABLE `transaction`
  DROP COLUMN xml_def_file;

# ALTER TABLE `transaction`
#         ADD KEY `transaction_ibfk_2` (`swift_search`),
#         ADD CONSTRAINT `transaction_ibfk_2` FOREIGN KEY (`swift_search`) REFERENCES `swift_search_definition` (`swift_search_definition_id`);

-- @UNDO

#TBD