#!/bin/bash

VER=3.5-SNAPSHOT
echo Starting Swift configuration

# Absolute path to this script
SCRIPT=`readlink -f $0`
# Absolute path to directory this script is in
SCRIPTPATH=`dirname $SCRIPT`

java  -Dswift.home="${SCRIPTPATH}" -Dswift.daemon=config -cp "bin/swift/*" edu.mayo.mprc.launcher.Launcher --config --war ./bin/swift/swift-web-${VER}.war  $*
