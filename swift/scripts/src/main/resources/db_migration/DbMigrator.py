#!/usr/bin/python
#This is the main program that controls the execution of dbmigrator.

import argparse
import os
import glob
import re
import datetime


def createGeneratedFilePath(scriptDir):
    sqlDirectory = os.path.join(os.path.abspath(args.dir), "migrations")
    makeDirsSafe(sqlDirectory)

    for i in range(0, 1000):
        migrationFileName = "migration_{0}-{1}_{2}.sql".format(str(args.start), str(args.end), str(i))
        candidate = os.path.join(sqlDirectory, migrationFileName)
        print(candidate)
        if (not os.path.exists(candidate)):
            return candidate


versionSqlFilePattern = re.compile(r"(\d+)_.*\.sql")


def catalogSqlFiles(toCatalog):
    catalog = {}
    for f in toCatalog:
        m = re.search(versionSqlFilePattern, f)
        if (m):
            catalog[int(m.group(1))] = f
    return catalog;


def makeDirsSafe(dir):
    try:
        os.makedirs(dir)
    except OSError:
        pass


class FileAppender(object):
    def __init__(self, path):
        self.path = path

    def appendFileContents(self, fromFilePath, isForward):
        openFrom = open(fromFilePath, 'r')
        content = openFrom.read();
        openFrom.close()

        matchSplit = content.split("-- @UNDO")

        if (isForward):
            self.appendString(matchSplit[0])
        else:
            self.appendString(matchSplit[1])

    def appendConcatFile(self, concatFilePath, version):
        openConcatFile = open(concatFilePath)
        content = openConcatFile.read()
        openConcatFile.close()

        content = content.replace("{scriptFile}", scriptFilePath)
        content = content.replace("{version}", str(version))
        self.appendString(content)

    def appendString(self, strContent):
        openTo = open(self.path, 'a')
        openTo.write(strContent)
        openTo.close()


parser = argparse.ArgumentParser("Create database migration script.")

parser.add_argument("-s", "--startVersion", dest="start", type=int, default=-1, help="version database is starting on")
parser.add_argument("--endVersion", "-e", dest="end", type=int, default=10000, help="version we want to end up on")
parser.add_argument("--directoryWithScripts", "-d", dest="dir", default=".",
                    help="the directory that contains all of the scripts")

args = parser.parse_args()

sqlFiles = glob.glob(os.path.join(args.dir, "*.sql"))

sqlFileCatalog = catalogSqlFiles(sqlFiles)

isForwardScript = args.start < args.end

if (isForwardScript):
    versionsToExecute = range(args.start + 1, args.end + 1, 1)
else:
    versionsToExecute = range(args.start, args.end, -1)

generatedFilePath = createGeneratedFilePath(args.dir)

concatFilePath = os.path.join(args.dir, "concat.sql")

fileAppender = FileAppender(generatedFilePath)
fileAppender.appendString("-- Created {0} by DbMigrator.".format(datetime.datetime.now().isoformat()))
for version in versionsToExecute:
    if (version in sqlFileCatalog):
        scriptFilePath = sqlFileCatalog[version]
        fileAppender.appendString('\n-- ' + scriptFilePath + '\n')
        fileAppender.appendFileContents(scriptFilePath, isForwardScript)
        fileAppender.appendConcatFile(concatFilePath, version if isForwardScript else version - 1);
    else:
        print("Warning there was no file for step " + str(version))

print(generatedFilePath)

