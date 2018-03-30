#!/bin/bash
#*****************************************************
# IIGFeed run script                     R Moulton
#*****************************************************

#-------- If port 2020 is open, job is already running
nmap localhost | grep '2020/tcp open' >& /dev/null
if [ $? -eq 0 ]
then
   exit 0
fi

ROOT=/home/rmoult01/NetBeansProjects
PKG=IIGFeed
CLASS=edu.wustl.mir.mars.iig.IIGFeed

CLSPTH=$ROOT/MarsDB/dist/MarsDB.jar:$ROOT/$PKG/dist/$PKG.jar

cd $ROOT/$PKG/runDirectory

echo $CLSPTH

java -cp $CLSPTH $CLASS &