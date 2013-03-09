@echo off
set VER=3.0-SNAPSHOT
echo Starting Swift configuration
java  -Dlog4j.configuration=file:conf/log4j.properties  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5200 -cp "bin\swift\*"  edu.mayo.mprc.launcher.Launcher --config --war .\bin\swift\swift-%VER%.war %*
