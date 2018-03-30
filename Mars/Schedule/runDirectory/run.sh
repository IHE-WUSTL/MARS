#!/bin/bash
#*****************************************************
# Schedule run script                        R Moulton
#*****************************************************

ROOT=/home/rmoult01/NetBeansProjects
PKG=Schedule
CLASS=edu.wustl.mir.mars.schedule.Schedule

CLSPTH=$ROOT/MarsDB/dist/MarsDB.jar:$ROOT/$PKG/lib/*:$ROOT/$PKG/dist/$PKG.jar

cd $ROOT/$PKG/runDirectory

echo $CLSPTH

java -cp $CLSPTH $CLASS &