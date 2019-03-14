# IBM MQ Monitor for New Relic Infrastructure

## Summary

The IBM  MQ monitor connects to all configured  MQ queue managers for queue and channel statistics and reports them to New Relic Insights.

The monitor connects to each queueManager specified in the plugin.json.


## Monitor requirements

1. Java Runtime version 1.8 or later
2. IBM WebSphere MQ v7 or later. MQ Java Client libraries are not distributed along with this monitor. They must be supplied though the CLASSPATH entry (see start up script- start.sh)


## Installation

Unzip the ibm-mq-infra-monitor.tar.gz file into the monitor home folder.

```
cd {ibm-mq-infra-monitor}
tar xvf  ibm-mq-infra-monitor.tar.gz
```


The control script - start.sh and mqmonitor - is used for starting, stopping and getting status.
Update start.sh as follows

* Edit the APP_HOME variable in the start.sh script.

* Update the MQ_LIB variable to specify the folder where the MQ java client libraries are located.

### Setup the monitor to run as service

Setting up the application to run as a Java Service is dependent on the platform. Please contact your platform administrator for help setting this up.


## Configuration

### plugin.json

Rename the plugin.template.json to plugin.json. Edit all parameters to your environment. 

The "global" object can contain the overall plugin properties:

* "account_id" - your new relic account id. You can find it in the URL that you use to access newrelic. For example: https://rpm.newrelic.com/accounts/{accountID}/applications
* "insights_mode" - The insights_insert_key provided here will be used to post metrics to New Relic.
* "proxy" - Enter the proxy setting in this section if a proxy is required. See more detail later on in this document.
* "queueIgnores" -  An array of "ignoreRegEx" objects. The value of the object is a regular expression. Any queue name on any queue manager that matches the regular expression will be ignored (i.e. no metrics collected). The array can contain any number of entries.
* "queueIncludes" - Overrides queueIgnores with same format. This allows wildcard excludes but then the ability to explicitly include specific queues here.


The "agents" object contains an array of “agent” objects. Each “agent” object has the following properties.

* "name" – any descriptive name for the queue manager
* "eventType" - Optional EventType to use for this agent (Default: `IBMMQSample`)
* "host" – hostname or IP for the queue manager
* "port" – port number that the queue manager is listening on
* “queueManager” - the name of the MQ queue manager to connect to.
* "channel" - channel name used to connect to the queue manager. Typically you can use SYSTEM.DEF.SVRCONN
* "username" - username used to connection (optional if not needed)
* "password" – password used to connection (optional if not needed)
* "reportEventMessages" - true to report MQ Event messages.
* "reportMaintenanceErrors" - true to report errors with MQ Tools daily maintenance processes.
* "reporQueueEventStatus" - true to report MQ Queue status
* "dailyMaintenanceErrorScanTime" - HH:MM to scan for maintenance errors each day.  This should be a time shortly after the daily maintenance processes run.
* "mqToolsLogPath" - The logPath to the MQ Tools logs. Ex: /var/mqm/mqtools_log.
* "monitorErrorLogs" - true to gather select metrics from the queue manager error logs.
* "errorLogPath" - Location of queue manager error logs. Only required when monitorErrorLogs=true.
* "agentTempPath" - A directory where the agent can read/write temp files for it's own use.

```
{
    "global": {
    	"account_id": "insert_your_RPM_account_ID_here",
	"insights_mode": {
		"insights_insert_key": "insert_your_insights_insert_key_here"
	},
	"dashboards": {
		"admin_api_key": "insert_your_admin_api_key_here",
		"installer_url": "insert_your_installer_url_or_leave_blank",
		"integration_guid": "insert_your_integration_guid_here",
		"dashboard_install": "normal"			
	},
        "queueIgnores": 
         [    "SYSTEM\\..*", 
              "AMQ.\\d\\d.*", 
              "Amq\\.Mqexplorer\\..*"
         ]
    },
    "agents": [
               {
                   "name": "any descriptive name for this queue manager",
                   "eventType": "IBMMQSample",
                   "host": "queue manager host name to connect to",
                   "port": 1414,
                   "username": "",
                   "password": "",
                   "queueManager": "queue manager name",
                   "channel": "SYSTEM.DEF.SVRCONN",
                   "queueIgnores": []
               }
    ]
}
```


## Integration with New Relic Insights 

Add the following additional properties

```
	"insights_mode": {
		"insights_insert_key": "enter-your-api-key"
	},
	"proxy": {
			"proxy_host": "enter_proxy_host",
			"proxy_port": 443,
			"proxy_username": "enter_proxy_username",
			"proxy_password": "enter_proxy_password"
	}
```

## Logging

The logging configuration can be controlled using the logback configuration file- ./config/logback.xml

Edit the following block of XML at the end of the logback.xml to change the log level (possible values are INFO, DEBUG, ERROR) and the log ouput(possible values are STDOUT, FILE)
```
    <logger name="com.newrelic" level="DEBUG" additivity="false">
        <appender-ref ref="STDOUT" />
    </logger>
```

## Metrics and Dashboarding
All metrics collected by this plugin are reported as events of type "IBMMQSample". 


## Event Attibutes
This integration collects and generates Channel and Queue performance metrics which are indicated by **entity** attribute.
 
#### Entity  : Channel

```
{
  "results": [
    {
      "stringKeys": [
        "agentName",
        "channelName",
        "channelStatus",
        "channelType",
        "connectionName",
        "entity",
        "hostname",
        "nr.customEventSource",
        "provider",
        "qManagerHost",
        "qManagerName"
      ],
      "numericKeys": [
        "bufferRecCount",
        "bufferRecRate",
        "buffersSentCount",
        "buffersSentRate",
        "bytesRecCount",
        "bytesRecRate",
        "bytesSentCount",
        "messageCount",
        "messageRate",
        "timestamp"
      ],
      "booleanKeys": [],
      "allKeys": [
        "agentName",
        "bufferRecCount",
        "bufferRecRate",
        "buffersSentCount",
        "buffersSentRate",
        "bytesRecCount",
        "bytesRecRate",
        "bytesSentCount",
        "channelName",
        "channelStatus",
        "channelType",
        "connectionName",
        "entity",
        "hostname",
        "messageCount",
        "messageRate",
        "nr.customEventSource",
        "provider",
        "qManagerHost",
        "qManagerName",
        "timestamp"
      ]
    }
  ]
}
```

#### Entity  : Queue

```
{
  "results": [
    {
      "stringKeys": [
        "agentName",
        "hostname",
        "lastGet",
        "lastPut",
        "nr.customEventSource",
        "object",
        "provider",
        "qManagerHost",
        "qManagerName",
        "qName"
      ],
      "numericKeys": [
        "highQDepth",
        "msgDeqCount",
        "msgEnqCount",
        "oldestMsgAge",
        "openInputCount",
        "openOutputCount",
        "qDepth",
        "qDepthMax",
        "qDepthPercent",
        "timeSinceReset",
        "timestamp",
        "uncommittedMsgs"
      ],
      "booleanKeys": [],
      "allKeys": [
        "agentName",
        "highQDepth",
        "hostname",
        "lastGet",              // reportQueueStatus must be enabled
        "lastPut",              // reportQueueStatus must be enabled
        "msgDeqCount",
        "msgEnqCount",
        "nr.customEventSource",
        "object",
        "oldestMsgAge",        // reportQueueStatus must be enabled
        "openInputCount",
        "openOutputCount",
        "provider",
        "qDepth",
        "qDepthMax",
        "qDepthPercent",
        "qManagerHost",
        "qManagerName",
        "qName",
        "timeSinceReset",
        "timestamp",
        "uncommittedMsgs"     // reportQueueStatus must be enabled
      ]
    }
  ]
}
```