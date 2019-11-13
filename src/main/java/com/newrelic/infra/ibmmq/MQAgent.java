package com.newrelic.infra.ibmmq;

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
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.newrelic.infra.ibmmq.constants.EventConstants;
import com.newrelic.infra.ibmmq.constants.ObjectStatusSampleConstants;
import com.newrelic.infra.ibmmq.constants.QueueSampleConstants;
import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.InventoryReporter;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;

public class MQAgent extends Agent {
	public static final String DEFAULT_SERVER_HOST = "localhost";
	public static final int DEFAULT_SERVER_PORT = 1414;

	private AgentConfig agentConfig = null;

	private QueueMetricCollector queueMetricCollector = null;
	private TopicMetricCollector topicMetricCollector = null;
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
		this.topicMetricCollector  = new TopicMetricCollector(agentConfig);
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
			}  catch (MQException e) {
				reportQueueManagerHostNotResponding(agentConfig.getServerQueueManagerName(), "QUEUE_MANAGER_NOT_AVAILABLE", e.reasonCode, metricReporter);
				logger.error("Problem creating MQQueueManager", e);
				return;
			} catch (Throwable t) {
				reportQueueManagerHostNotResponding(agentConfig.getServerQueueManagerName(), "QUEUE_MANAGER_NOT_AVAILABLE", 1 , metricReporter);
				logger.error("Problem creating MQQueueManager", t);
				return;
			}
			try {
				logger.debug("model queue:"+agentConfig.getModelQueue());
				logger.debug("command queue:"+agentConfig.getCommandQueue());
				agent = new PCFMessageAgent();
//				hren add modelqueue support
				if (!agentConfig.getModelQueue().isEmpty())
				{
					agent.setModelQueueName(agentConfig.getModelQueue());

				}
//				hren add commandqueue support 
				if (!agentConfig.getCommandQueue().isEmpty())
				{
					agent.connect(mqQueueManager, agentConfig.getCommandQueue(), (String) null);
				}else {
					agent.connect(mqQueueManager);
				}
			} catch (PCFException e) {
				reportQueueManagerHostNotResponding(agentConfig.getServerQueueManagerName(), "QUEUE_MANAGER_CONNECT_ERROR", e.reasonCode, metricReporter);
				logger.error("Problem creating PCFMessageAgent:PCFException ", e);
				return;
			} catch (com.ibm.mq.headers.MQExceptionWrapper e) {
				reportQueueManagerHostNotResponding(agentConfig.getServerQueueManagerName(), "QUEUE_MANAGER_CONNECT_ERROR", e.reasonCode, metricReporter);
				logger.error("Problem creating PCFMessageAgent:MQExceptionWrapper", e);
				return;
			} catch (Throwable t) {
				reportQueueManagerHostNotResponding(agentConfig.getServerQueueManagerName(), "QUEUE_MANAGER_CONNECT_ERROR", 1, metricReporter);
				logger.error("Problem creating PCFMessageAgent:Throwable", t);
				return;
			}
			
			queueManagerMetricCollector.reportQueueManagerStatus(agent, metricReporter);
			clusterMetricCollector.reportClusterQueueManagerSuspended(agent, metricReporter);
			listenerMetricCollector.reportListenerStatus(agent, metricReporter);
			
			Map<String, List<Metric>> metricMap = new HashMap<>();

			//hren handle queueToPolls logic, only send PCFMessage to those queues in queueToPolls 
			logger.debug(" queueToPolls:"+agentConfig.queueToPolls);
			
			for (String queueToPoll : agentConfig.queueToPolls) {
				logger.debug("About to to poll Queue:"+queueToPoll);
				queueMetricCollector.reportQueueStats(agent, metricReporter, metricMap, queueToPoll);
				queueMetricCollector.addResetQueueStats(agent, metricReporter, metricMap,queueToPoll);
	            if (agentConfig.reportAdditionalQueueStatus()) {
	                queueMetricCollector.addQueueStatusStats(agent, metricReporter, metricMap,queueToPoll);
	            }
			}
			
			for (Map.Entry<String, List<Metric>> entry : metricMap.entrySet()) {
				metricReporter.report(QueueSampleConstants.MQ_QUEUE_SAMPLE, entry.getValue());
			}

			

			// end handle queueToPoll logic
			
			channelMetricCollector.reportChannelStats(agent, metricReporter);
			
			if (agentConfig.reportTopicStatus()) {
				topicMetricCollector.reportTopicStatus(agent, metricReporter);
			}
			if (agentConfig.reportAdditionalTopicStatus() ) {
				topicMetricCollector.reportTopicStatusSub(agent, metricReporter);
			}

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
	private MQQueueManager connect() throws MQException  {
//		set SSL system properties
		if (!agentConfig.getSslTrustStore().isEmpty())
		{
			logger.debug(" javax.net.ssl.trustStore:"+agentConfig.getSslTrustStore());
			System.setProperty("javax.net.ssl.trustStore",agentConfig.getSslTrustStore());
			System.setProperty("javax.net.ssl.trustStorePassword",agentConfig.getSslTrustStorePassword());
		}

		if (!agentConfig.getSslKeyStore().isEmpty())
		{
			logger.debug(" javax.net.ssl.keyStore:"+agentConfig.getSslKeyStore());
			System.setProperty("javax.net.ssl.keyStore",agentConfig.getSslKeyStore());
			System.setProperty("javax.net.ssl.keyStorePassword",agentConfig.getSslKeyStorePassword());
		}
		
		MQEnvironment.hostname = agentConfig.getServerHost();
		MQEnvironment.port = agentConfig.getServerPort();
		MQEnvironment.userID = agentConfig.getServerAuthUser();
		MQEnvironment.password = agentConfig.getServerAuthPassword();
		MQEnvironment.channel = agentConfig.getServerChannelName();
//		add cipherSuite for SSL support hren
		if (!agentConfig.getcipherSuite().isEmpty())
		{
			MQEnvironment.sslCipherSuite=agentConfig.getcipherSuite();
		}
		MQQueueManager qMgr = new MQQueueManager(agentConfig.getServerQueueManagerName());
		
		MQEnvironment.properties.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);

		return qMgr;
	}

	// Often times a code lookup will result in a lengthy description like
	// abc/xyz/someValue and we just want someValue.
	public static String friendlyCodeLookup(int code, String filter) {
		String desc = MQConstants.lookup(code, filter);
		int index = desc.lastIndexOf('/');
		return index == -1 ? desc : desc.substring(index + 1);
	}
	
	private void reportQueueManagerHostNotResponding(String queueManagerName, String errormessage, int reasoncode, MetricReporter metricReporter) {
		List<Metric> metricset = new LinkedList<>();
        metricset.add(new AttributeMetric(EventConstants.PROVIDER, EventConstants.IBM_PROVIDER));
        metricset.add(new AttributeMetric(EventConstants.Q_MANAGER_NAME, agentConfig.getServerQueueManagerName()));
        metricset.add(new AttributeMetric(EventConstants.Q_MANAGER_HOST, agentConfig.getServerHost()));
		
		metricset.add(new AttributeMetric(EventConstants.OBJECT_ATTRIBUTE, EventConstants.OBJ_ATTR_TYPE_Q_MGR));
		metricset.add(new AttributeMetric(ObjectStatusSampleConstants.CHNL_INIT_STATUS, errormessage));
		metricset.add(new AttributeMetric(ObjectStatusSampleConstants.CMD_SERVER_STATUS, errormessage));
		metricset.add(new AttributeMetric(EventConstants.STATUS, errormessage));
		metricset.add(new AttributeMetric(EventConstants.ERROR, reasoncode));
		metricset.add(new AttributeMetric(EventConstants.NAME, queueManagerName));
		metricReporter.report(ObjectStatusSampleConstants.MQ_OBJECT_STATUS_SAMPLE, metricset);
	}

}