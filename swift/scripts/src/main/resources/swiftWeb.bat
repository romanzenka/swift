@echo off
set VER=3.5-SNAPSHOT
echo Starting Swift web server
java -cp "bin\swift\*" edu.mayo.mprc.launcher.Launcher --war .\bin\swift\swift-web-%VER%.war %*
