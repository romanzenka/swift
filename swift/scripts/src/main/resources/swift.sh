#!/bin/bash
echo Running Swift daemon
umask 002
VER=3.5-SNAPSHOT
java -Dswift.home=`pwd` -Dlog4j.configuration=file:conf/log4j.properties -cp "bin/swift/*" edu.mayo.mprc.swift.Swift $*
