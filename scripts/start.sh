#!/bin/bash

#update the APP_HOME 
export APP_HOME=.

#update the location where MQ Client libraries are located
export MQ_LIB=

#Use one of the following args to start the monitor as a service that can be invoked by the infra plugin OR as a service reporting directly to Insights
ARGS="-Dnewrelic.platform.config.dir=$APP_HOME/config -Dnewrelic.platform.service.mode=RPC"
#ARGS="-Dnewrelic.platform.config.dir=$APP_HOME/config -Dnewrelic.platform.service.mode=INSIGHTS"

# ***********************************************
# DO NOT EDIT BELOW THIS LINE
# ***********************************************

export CLASSPATH=$APP_HOME/config:$APP_HOME/plugin.jar:$MQ_LIB/com.ibm.mq.commonservices.jar:$MQ_LIB/com.ibm.mq.headers.jar:$MQ_LIB/com.ibm.mq.jar:$MQ_LIB/com.ibm.mq.jmqi.jar:$MQ_LIB/com.ibm.mq.pcf.jar:$MQ_LIB/com.ibm.mqjms.jar:$MQ_LIB/connector.jar

MAIN_CLASS=com.newrelic.infra.ibmmq.MQRunnerMain

exec java $ARGS -cp $CLASSPATH $MAIN_CLASS