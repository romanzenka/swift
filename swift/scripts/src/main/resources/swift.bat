@echo off
set VER=3.5-SNAPSHOT
echo Running Swift daemon
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5122 -cp "bin\swift\*" edu.mayo.mprc.swift.Swift %*
