#!/bin/bash
#*****************************************************
# Schedule run script                        R Moulton
# Designed to be run as a cron job.
# The option java.awt.headless is set to true to let
# Schedule know that no graphics environment is
# available.
#*****************************************************

ROOT=/home/rmoult01/NetBeansProjects
PKG=Schedule
CLASS=edu.wustl.mir.mars.schedule.Schedule

CLSPTH=$ROOT/MarsDB/dist/MarsDB.jar:$ROOT/$PKG/lib/*:$ROOT/$PKG/dist/$PKG.jar

OPTS=java.awt.headless="true"

cd $ROOT/$PKG/runDirectory

echo $CLSPTH

java -D$OPTS -cp $CLSPTH $CLASS &

#----------------------- delete reports over two weeks old
find reports -type f -name "*.xlsx" -mtime +14 -delete