package com.newrelic.infra.ibmmq;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.InventoryReporter;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;

public class MQAgent extends Agent {
	public static final int LATEST_VERSION = 2;
	public static final String DEFAULT_SERVER_HOST = "localhost";
	public static final int DEFAULT_SERVER_PORT = 1414;

	public static final String DEFAULT_EVENT_TYPE = "IBMMQSample";
	
	private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM HH:mm:ss");

	private AgentConfig agentConfig = null;

	private QueueMetricCollector queueMetricCollector = null;
	private QueueManagerMetricCollector queueManagerMetricCollector = null;
	private ListenerMetricCollector listenerMetricCollector = null;
	private ChannelMetricCollector channelMetricCollector = null;
	private ClusterMetricCollector clusterMetricCollector = null;
	private EventMetricCollector eventMetricCollector = null;
	private LogMetricCollector logMetricCollector = null;

	private static final Logger logger = LoggerFactory.getLogger(MQAgent.class);
	
	public MQAgent(AgentConfig agentConfig, String dailyMaintenanceErrorScanTime) {
		super();
		this.agentConfig  = agentConfig;
		this.queueMetricCollector  = new QueueMetricCollector(agentConfig);
		this.queueManagerMetricCollector  = new QueueManagerMetricCollector(agentConfig);
		this.listenerMetricCollector  = new ListenerMetricCollector(agentConfig);
		this.channelMetricCollector  = new ChannelMetricCollector(agentConfig);
		this.clusterMetricCollector = new ClusterMetricCollector(agentConfig);
		this.eventMetricCollector = new EventMetricCollector(agentConfig);
		this.logMetricCollector = new LogMetricCollector(agentConfig);
		logMetricCollector.setDailyMaintenanceErrorScanTime(dailyMaintenanceErrorScanTime);
	}

	@Override
	public void dispose() throws Exception {
	}

	@Override
	public void populateInventory(InventoryReporter inventoryReporter) throws Exception {
	}

	@Override
	public void populateMetrics(MetricReporter metricReporter) throws Exception {
		MQQueueManager mqQueueManager = null;
		PCFMessageAgent agent = null;
		try {
			try {
				mqQueueManager = connect();
			} catch (Exception e) {
				reportEventMetric(new Date(), null, agentConfig.getServerQueueManagerName(), "QUEUE_MANAGER_NOT_AVAILABLE", null,
						e.getMessage(), metricReporter);
				logger.error("Problem creating MQQueueManager", e);
				return;
			}
			try {
				agent = new PCFMessageAgent(mqQueueManager);
				agent.connect(mqQueueManager);
			} catch (Exception e) {
				reportEventMetric(new Date(), null, agentConfig.getServerQueueManagerName(), "COMMAND_SERVER_NOT_RESPONDING", null,
						e.getMessage(), metricReporter);
				logger.error("Problem creating PCFMessageAgent", e);
				return;
			}

			Map<String, List<Metric>> metricMap = new HashMap<>();
			queueMetricCollector.reportQueueStats(agent, metricReporter, metricMap);
			queueMetricCollector.reportResetQueueStats(agent, metricReporter, metricMap);
			for (Map.Entry<String, List<Metric>> entry : metricMap.entrySet()) {
				metricReporter.report("MQQueueSample", entry.getValue());
			}
			channelMetricCollector.reportChannelStats(agent, metricReporter);
			queueManagerMetricCollector.reportQueueManagerStatus(agent, metricReporter);
			clusterMetricCollector.reportClusterQueueManagerSuspended(agent, metricReporter);
			listenerMetricCollector.reportListenerStatus(agent, metricReporter);
			//TODO collect topic metrics with MQCMD_INQUIRE_TOPIC_STATUS

			if (agentConfig.reportEventMessages()) {
				eventMetricCollector.reportEventStats(mqQueueManager, metricReporter);
			}
			if (agentConfig.reportMaintenanceErrors()) {
				logMetricCollector.checkForCompressionError(mqQueueManager, metricReporter);
			}
			if (agentConfig.monitorErrorLogs()) {
				logMetricCollector.reportErrorLogEvents(mqQueueManager, metricReporter);
			}
		} finally {
			try {
				if (agent != null) {
					agent.disconnect();
				}
				if (mqQueueManager != null) {
					mqQueueManager.disconnect();
				}
			} catch (MQException ex) {
			}
		}
	}

	@SuppressWarnings("unchecked")
	private MQQueueManager connect() throws MQException {
		MQEnvironment.hostname = agentConfig.getServerHost();
		MQEnvironment.port = agentConfig.getServerPort();
		MQEnvironment.userID = agentConfig.getServerAuthUser();
		MQEnvironment.password = agentConfig.getServerAuthPassword();
		MQEnvironment.channel = agentConfig.getServerChannelName();
		MQQueueManager qMgr = new MQQueueManager(agentConfig.getServerQueueManagerName());

		MQEnvironment.properties.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);

		return qMgr;
	}

	//TODO get rid of this method
	private void reportEventMetric(Date dateTime, String eventQueueName, String queueManager, String reason,
			String reasonQualifier, String details, MetricReporter metricReporter) {
		List<Metric> metricset = new LinkedList<>();

		metricset.add(new AttributeMetric("putTime", dateTimeFormat.format(dateTime)));
		metricset.add(new AttributeMetric("eventQueue", eventQueueName));
		metricset.add(new AttributeMetric("queueManager", queueManager));
		metricset.add(new AttributeMetric("reasonCode", reason));
		metricset.add(new AttributeMetric("reasonQualifier", reasonQualifier));
		metricset.add(new AttributeMetric("details", details));

		metricReporter.report("MQEventSample", metricset);
	}

	// Often times a code lookup will result in a lengthy description like
	// abc/xyz/someValue and we just want someValue.
	public static String friendlyCodeLookup(int code, String filter) {
		String desc = MQConstants.lookup(code, filter);
		int index = desc.lastIndexOf('/');
		return index == -1 ? desc : desc.substring(index + 1);
	}

}