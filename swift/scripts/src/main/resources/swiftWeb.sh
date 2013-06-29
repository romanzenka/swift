#!/bin/bash

VER=3.5-SNAPSHOT
echo Starting Swift web server
umask 002
java -Dswift.home=`pwd` -Dlog4j.configuration=file:conf/log4j.properties -cp "bin/swift/*" edu.mayo.mprc.launcher.Launcher --war ./bin/swift/swift-web-${VER}.war $*
