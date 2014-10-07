require 'rubygems'
require 'mysql2'
require 'json'
require 'pp'

version  = 1
host     = "node029.mprc.mayo.edu"
database = "swift_heme"
user     = "swift_heme_admin"
pass     = "heme$admin"
### SQL USED ###
# ALTER TABLE `swift_heme`.`curation` ADD steps_json MEDIUMTEXT;



client = Mysql2::Client.new(
    :host=>"#{host}",
    :port=>3306,
    :username=>"#{user}",
    :password=>"#{pass}",
    :database => "#{database}")
nxline = client.escape("\n")


newDocuments = Hash.new

### Get & Set Curation Step Table ###
cSteps = client.query("SELECT * FROM curation_step", :symbolize_keys => true)
cSteps.each do |row|
  id=row[:step_id]
  newDocuments[id]=Hash.new
  newDocuments[id]['step']=id
  newDocuments[id]['last_run_completion_count']=row[:last_run_completion_count]
end


tables=[
    'curation_step_database_upload',
    'curation_step_header_filter',
    'curation_step_header_transform',
    'curation_step_make_decoy',
    'curation_step_manual_inclusion',
    'curation_step_new_db'
]
identifiers=[
    'upload_id',
    'header_filter_id',
    'header_transform_id',
    'sequence_manipulation_id',
    'manual_inclusion_id',
    'new_database_id'
]


tables.each_with_index do |tb, i|
  res = client.query("SELECT * FROM #{tb}")  # Query Table
  res.each do |row|                          # Loop rows
    id = row[identifiers[i]]                 # Key to Parent Table

    row.each do |key, value|                 # Loop & Set columns other than xxx_id
      next if key =~ /_id$/
      fixedVal =  value.to_s.gsub("\|", "\\|")  ## need to handle "\|" issue for GSON
      newDocuments[id][key]= fixedVal
    end
    newDocuments[id]['step_type']=tb.gsub("curation_step_", "")
  end
end



curationAggregation = Hash.new
### Get & Set Curation Step Table ###
cSteps = client.query("SELECT * FROM curation_step_list", :symbolize_keys => true)
cSteps.each do |row|
  s_id=row[:step_id]
  newDocuments[s_id]['sort_order']=row[:sort_order]
  if curationAggregation[row[:curation_id]].kind_of?(Array)
    curationAggregation[row[:curation_id]].push(newDocuments[s_id])
  else
    curationAggregation[row[:curation_id]] = [ newDocuments[s_id] ]
  end


end




### NOW INSERT JSON BLOCKS BACK INTO DB ###
curationAggregation.each do |cur_id, stepArray|
  curationStepObj = Hash.new
  curationStepObj['version'] = version
  curationStepObj['steps'] = stepArray

  client.query("UPDATE curation SET steps_json = '#{curationStepObj.to_json}' WHERE curation_id = #{cur_id}")
end