@ECHO OFF

set APP_HOME=.

set MQ_LIB="C:\\Program Files (x86)\\IBM\\WebSphere MQ\\java\\lib"

rem Use one of the following args to start the monitor as a service that can be invoked by the infra plugin OR as a service reporting directly to Insights
set ARGS="-Dnewrelic.platform.config.dir=%APP_HOME%\config" -Dnewrelic.platform.service.mode=RPC
rem set ARGS="-Dnewrelic.platform.config.dir=%APP_HOME%\config" -Dnewrelic.platform.service.mode=INSIGHTS

rem ***********************************************
rem DO NOT EDIT BELOW THIS LINE
rem ***********************************************

set CLASSPATH=%APP_HOME%\config;%APP_HOME%\plugin.jar;%MQ_LIB%\com.ibm.mq.commonservices.jar;%MQ_LIB%\com.ibm.mq.headers.jar;%MQ_LIB%\com.ibm.mq.jar;%MQ_LIB%\com.ibm.mq.jmqi.jar;%MQ_LIB%\com.ibm.mq.pcf.jar;%MQ_LIB%\com.ibm.mqjms.jar;%MQ_LIB%\connector.jar

set MAIN_CLASS=com.newrelic.infra.ibmmq.MQRunnerMain

java %ARGS% -Xms128m -Xmx384m -Xnoclassgc -cp %CLASSPATH% %MAIN_CLASS%