require 'rubygems'
require 'mysql2'
require 'json'
require 'pp'

version  = 1
host = "atlas.mayo.edu"
database = "atlas_dev"
user = "swift"
pass = "###"

### Define Methods ####
def help_statment()
  str="Usage: ruby createDocStore_curator_steps.rb \n"
  str+="\tThis script pulls the curation step tables, reformats then json stores them in the curation table\n"
  str+="\t-t <host>\n\t-d <db>"
  str+="\t-u <user>\n\t-p <password> [Required]"
  return str
end


#### Help Statement ####
if ARGV[0]=='-h' || ARGV[0]=='--help' || ARGV.size==0
  puts help_statment
  exit 0
end
opt=Hash[*ARGV]



#### Open Directories & Files ####
if !opt.has_key?('-p')
  puts "Missing -p option: Please provide your password"
  exit 1
else
  pass = opt['-p']
end
if opt.has_key?('-t')
  host = opt['-t']
end
if opt.has_key?('-d')
  database = opt['-d']
end
if opt.has_key?('-u')
  user = opt['-u']
end


mysql2javaMap = {
  "criteria_string" => "criteriaString",
  "text_mode" => "textMode",
  "match_mode" => "matchMode",
  "inclusion_header" => "header",
  "inclusion_sequence" => "sequence",
  "overwrite_mode" => "overwriteMode",
  "manipulator_type" => "manipulatorType",
  "server_path" => "pathToUploadedFile",
  "client_path" => "fileName",
  "md5_checksum" => "md5CheckSum",
  "match_pattern" => "matchPattern",
  "substitution_pattern" => "substitutionPattern",
  "last_run_completion_count" => "lastRunCompletionCount"
}


### SQL USED ###
# ALTER TABLE `swift_heme`.`curation` ADD steps_json MEDIUMTEXT;



client = Mysql2::Client.new(
    :host=>"#{host}",
    :port=>3306,
    :username=>"#{user}",
    :password=>"#{pass}",
    :database => "#{database}",
    :secure_auth => false)
nxline = client.escape("\n")


newDocuments = Hash.new

### Get & Set Curation Step Table ###
cSteps = client.query("SELECT * FROM curation_step", :symbolize_keys => true)
cSteps.each do |row|
  id=row[:step_id]
  newDocuments[id]=Hash.new
  newDocuments[id]['step']=id
  newDocuments[id]['lastRunCompletionCount']=row[:last_run_completion_count]
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
      if mysql2javaMap.has_key?(key)
        key = mysql2javaMap[key]
      end
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

  client.query("UPDATE curation SET steps_json = '#{curationStepObj.to_json.gsub(/\\/, '\&\&').gsub(/'/, "''")}' WHERE curation_id = #{cur_id}")
end