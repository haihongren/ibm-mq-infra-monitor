#!/bin/bash

#update the APP_HOME 
export APP_HOME=.

#update the location where MQ Client libraries are located
export MQ_LIB=

# ***********************************************
# DO NOT EDIT BELOW THIS LINE
# ***********************************************

export CLASSPATH=$APP_HOME/config:$APP_HOME/plugin.jar:$MQ_LIB/com.ibm.mq.commonservices.jar:$MQ_LIB/com.ibm.mq.headers.jar:$MQ_LIB/com.ibm.mq.jar:$MQ_LIB/com.ibm.mq.jmqi.jar:$MQ_LIB/com.ibm.mq.pcf.jar:$MQ_LIB/com.ibm.mqjms.jar:$MQ_LIB/connector.jar

MAIN_CLASS=com.newrelic.infra.ibmmq.Main

exec java $ARGS -cp $CLASSPATH $MAIN_CLASS