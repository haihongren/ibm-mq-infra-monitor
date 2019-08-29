#!/bin/bash

#update the APP_HOME 
export APP_HOME=.

#update the location where MQ Client libraries are located
export MQ_LIB=./mqlib

# ***********************************************
# DO NOT EDIT BELOW THIS LINE
# ***********************************************
export ARGS="-Dnewrelic.platform.config.dir=$APP_HOME/config"

export CLASSPATH=$APP_HOME/config:$APP_HOME/plugin.jar:$MQ_LIB/*

MAIN_CLASS=com.newrelic.infra.ibmmq.Main

exec java $ARGS -cp $CLASSPATH $MAIN_CLASS