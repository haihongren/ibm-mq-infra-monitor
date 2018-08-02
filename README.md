# IBM MQ Monitor for New Relic Infrastructure

## Summary

The IBM  MQ monitor connects to all configured  MQ queue managers for queue and channel statistics and reports them to New Relic Insights.

The monitor connects to each queueManager specified in the plugin.json.


## Monitor requirements

1. Java Runtime version 1.6 or later
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

### Service Mode

This monitor can be started in two modes as specified by the "-Dnewrelic.platform.service.mode" JVM parameter. 

* If this parameter equals "RPC", then the service starts up in RPC mode and listens for requests from the New Relic Infra Agent Integration Plugin. 

* If this parameter equals "INSIGHTS", then this monitor creates its own executor that will request metrics every minute and post them directly to Insights 

Update the 'ARGS' property in the start.sh script to change the service mode.

### Setup the monitor to run as service

Setting up the application to run as a Java Service is dependent on the platform. Please contact your platform administrator for help setting this up.


## Configuration

### plugin.json

Rename the plugin.template.json to plugin.json. Edit all parameters to your environment. 

The "global" object can contain the overall plugin properties:

* "account_id" - your new relic account id. You can find it in the URL that you use to access newrelic. For example: https://rpm.newrelic.com/accounts/{accountID}/applications
* "insights_mode" - If this monitor is started in Insights mode, then the insights_insert_key here will be used to post metric events.
* "infra_mode" - If this monitor is started in Infra (or RPC) mode, then this section contains the RPC service listen port. The infra agent plugin can then connect this to port to query metrics.
* "proxy" - Enter the proxy setting in this section if a proxy is required. See more detail later on in this document.
* "queueIgnores" -  a array of "ignoreRegEx" objects. The value of the object is a regular expression. Any queue name on any queue manager that matches the regular expression will be ignored (i.e. no metrics collected). The array can contain any number of entries.


The "agents" object contains an array of “agent” objects. Each “agent” object has the following properties.

* "name" – any descriptive name for the queue manager
* "eventType" - Optional EventType to use for this agent (Default: `IBMMQSample`)
* "host" – hostname or IP for the queue manager
* "port" – port number that the queue manager is listening on
* “queueManager” - the name of the MQ queue manager to connect to.
* "channel" - channel name used to connect to the queue manager. Typically you can use SYSTEM.DEF.SVRCONN
* "username" - username used to connection (optional if not needed)
* "password" – password used to connection (optional if not needed)
* "queueIgnores" – the same as the global queueIgnores except specific to this queue manager

```
{
    "global": {
    	"account_id": "insert_your_RPM_account_ID_here",
	"insights_mode": {
		"insights_insert_key": "insert_your_insights_insert_key_here"
	},
	"infra_mode": {
		"rpc_listener_port": 9001
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


## Integration with New Relic Insights (INSIGHTS mode only)

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

## Integration with Infrastructure Agent (RPC mode only)

1. Test the integration plugin 

    1. `bin/mq-plugin -mq_monitoring_service_port {port}`
    2. where {host} and {port} refer to the MQ Monitoring Service port.
2. Install the integration plugin

   1. Copy the bin folder (along with executable) and the definition file to /var/db/newrelic-infra/custom-integrations (Linux) or C:/Program Files/New Relic/newrelic-infra/custom-integrations (Windows)
   2. Copy the mq-monitor-config.yml file to /etc/newrelic-infra/integrations.d/ (Linux) or C:/Program Files/New Relic/newrelic-infra/integrations.d (Windows)
   3. Restart the infrastructure agent

Refer to [NR Infrastructure Documention](https://docs.newrelic.com/docs/infrastructure/integrations-sdk/getting-started/integration-file-structure) for more information about integration file structure


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
        "entity",
        "hostname",
        "nr.customEventSource",
        "provider",
        "qManagerHost",
        "qManagerName",
        "qName"
      ],
      "numericKeys": [
        "highQDepth",
        "msgDeqCount",
        "msgEnqCount",
        "openInputCount",
        "openOutputCount",
        "qDepth",
        "qDepthMax",
        "qDepthPercent",
        "timeSinceReset",
        "timestamp"
      ],
      "booleanKeys": [],
      "allKeys": [
        "agentName",
        "entity",
        "highQDepth",
        "hostname",
        "msgDeqCount",
        "msgEnqCount",
        "nr.customEventSource",
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
        "timestamp"
      ]
    }
  ]
}

```