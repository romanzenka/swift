@echo off
set VER=3.5-SNAPSHOT
echo Starting Swift web server
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5121 -cp "bin\swift\*" edu.mayo.mprc.launcher.Launcher --war .\bin\swift\swift-%VER%.war %*
