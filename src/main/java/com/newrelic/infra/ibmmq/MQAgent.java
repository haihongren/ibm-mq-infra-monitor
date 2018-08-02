package com.newrelic.infra.ibmmq;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.CMQXC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.MQCFH;
import com.ibm.mq.pcf.MQCFIN;
import com.ibm.mq.pcf.MQCFSL;
import com.ibm.mq.pcf.MQCFST;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.pcf.PCFParameter;
import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.InventoryReporter;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.GaugeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;
import com.newrelic.infra.publish.api.metrics.RateMetric;

public class MQAgent extends Agent {
	private static final String QUEUE_ACCESS_PROPERTY = "newrelic.queue.access.allow";
	private static final String DEFAULT_SERVER_HOST = "localhost";
	private static final String DEFAULT_EVENT_TYPE = "IBMMQSample";
	private static final int DEFAULT_SERVER_PORT = 1414;

	private static final Logger logger = LoggerFactory.getLogger(MQAgent.class);

	private boolean accessQueueMode = false;

	private String serverHost = DEFAULT_SERVER_HOST;
	private int serverPort = DEFAULT_SERVER_PORT;
	private String serverAuthUser = StringUtils.EMPTY;
	private String serverAuthPassword = StringUtils.EMPTY;
	private String serverChannelName = "SYSTEM.DEF.SVRCONN";
	private String serverQueueManagerName = null;
	private String eventType = DEFAULT_EVENT_TYPE;

	private List<Pattern> queueIgnores = new ArrayList<Pattern>();


	private static final Map<Integer,String> channelTypeMap;
	private static final Map<Integer,String> channelStatusMap;
	static{
		Map<Integer,String> sChannelStatus = new HashMap<Integer,String>();
		sChannelStatus.put(CMQCFC.MQCHS_BINDING, "BINDING");
		sChannelStatus.put(CMQCFC.MQCHS_STARTING, "STARTING");
		sChannelStatus.put(CMQCFC.MQCHS_RUNNING, "RUNNING");
		sChannelStatus.put( CMQCFC.MQCHS_PAUSED, "PAUSED");
		sChannelStatus.put(CMQCFC.MQCHS_STOPPING, "STOPPING");
		sChannelStatus.put(CMQCFC.MQCHS_RETRYING, "RETRYING");
		sChannelStatus.put(CMQCFC.MQCHS_STOPPED, "STOPPED");

		sChannelStatus.put(CMQCFC.MQCHS_REQUESTING, "REQUESTING");
		sChannelStatus.put(CMQCFC.MQCHS_DISCONNECTED, "DISCONNECTED");
		sChannelStatus.put(CMQCFC.MQCHS_INACTIVE, "INACTIVE");
		sChannelStatus.put(CMQCFC.MQCHS_INITIALIZING, "INITIALIZING");
		sChannelStatus.put(CMQCFC.MQCHS_SWITCHING, "SWITCHING");

		channelStatusMap = Collections.unmodifiableMap(sChannelStatus);

		Map<Integer,String> mChannelType = new HashMap<Integer,String>();
		mChannelType.put(CMQXC.MQCHT_SENDER, "SENDER");
		mChannelType.put(CMQXC.MQCHT_SERVER, "SERVER");
		mChannelType.put(CMQXC.MQCHT_RECEIVER, "RECEIVER");
		mChannelType.put(CMQXC.MQCHT_REQUESTER, "REQUESTER");
		mChannelType.put(CMQXC.MQCHT_CLNTCONN, "CLNTCONN");
		mChannelType.put(CMQXC.MQCHT_SVRCONN, "SVRCONN");
		mChannelType.put(CMQXC.MQCHT_ALL, "ALL");

		channelTypeMap = Collections.unmodifiableMap(mChannelType);
	}


	public void setServerQueueManagerName(String serverQueueManagerName) {
		this.serverQueueManagerName = serverQueueManagerName;
	}

	public void setServerHost(String host) {
		this.serverHost = StringUtils.isNotBlank(host) ? host : DEFAULT_SERVER_HOST;

	}
	public String getServerHost() {
		return this.serverHost;

	}

	public void setServerPort(int port) {
		this.serverPort = (port >0 && port < 65535) ? port : DEFAULT_SERVER_PORT;
//		logger.info("Using Server Port= {} ", this.serverPort);

	}
	public int getServerPort() {
		return this.serverPort;
	}

	public void setServerAuthUser(String serverAuthUser) {
		this.serverAuthUser = serverAuthUser;
	}

	public void setServerAuthPassword(String serverAuthPassword) {
		this.serverAuthPassword = serverAuthPassword;
	}

	public void setServerChannelName(String serverChannelName) {
		this.serverChannelName = serverChannelName;
	}

	public void setEventType(String eventType) {
		this.eventType = StringUtils.isNotBlank(eventType) ? eventType : DEFAULT_EVENT_TYPE;

	}
	public String getEventType() {
		return  this.eventType;
	}

	public MQAgent() {
		super();
		accessQueueMode =  BooleanUtils.toBoolean( System.getProperty(QUEUE_ACCESS_PROPERTY, "true"));
		logger.debug("Using newrelic.queue.access.allow ={} accessQueueMode={}", System.getProperty(QUEUE_ACCESS_PROPERTY, "true") , accessQueueMode);

	}

	@Override
	public void dispose() throws Exception {
	}

	@Override
	public void populateInventory(InventoryReporter inventoryReporter) throws Exception {
	}

	@Override
	public void populateMetrics(MetricReporter metricReporter) throws Exception {
        Map<String,List<Metric>> metricMap = new HashMap<String, List<Metric>>();
        MQQueueManager mqQueueManager = null;
        PCFMessageAgent agent =null;

        try {
			mqQueueManager = connect();
            agent = new PCFMessageAgent(mqQueueManager);

			if (accessQueueMode) {
                metricMap.putAll(reportQueueStats(mqQueueManager));
			} else {
				metricMap.putAll(reportQueueStatsLite(agent));
			}
            reportResetQueueStats(agent, metricMap);
			for (Map.Entry<String,List<Metric>> entry : metricMap.entrySet()){
				metricReporter.report(this.getEventType(), entry.getValue());
			}

            reportChannelStats(agent, metricReporter);


		} catch (MQException e) {
			logger.error("Error occured fetching metrics for {}:{}/{}" , this.getServerHost() , this.getServerPort() , serverQueueManagerName);
			throw e;
		}finally{
            try{
                if (agent !=null){
                    agent.disconnect();
                }
                if (mqQueueManager !=null) {
                    mqQueueManager.disconnect();
                }
            }catch(MQException ex){

            }

        }

	}



	@SuppressWarnings("unchecked")
	private MQQueueManager connect() throws MQException {
		MQEnvironment.hostname = this.getServerHost();
		MQEnvironment.port = this.getServerPort();
		MQEnvironment.userID = serverAuthUser;
		MQEnvironment.password = serverAuthPassword;
		MQEnvironment.channel = serverChannelName;
		MQQueueManager qMgr = new MQQueueManager(serverQueueManagerName);

		MQEnvironment.properties.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES_CLIENT);

		return qMgr;
	}

	private List<String> listQueues(MQQueueManager mqQueueManager) throws MQException, IOException  {

		List<String> queueList = new ArrayList<String>();

		PCFMessageAgent agent = new PCFMessageAgent();
		PCFParameter[] parameters = {
				new MQCFST(MQConstants.MQCA_Q_NAME, "*"),
				new MQCFIN(MQConstants.MQIA_Q_TYPE, MQConstants.MQQT_ALL)
		};

		agent.connect(mqQueueManager);
		MQMessage [] responses;

		responses = agent.send(MQConstants.MQCMD_INQUIRE_Q_NAMES, parameters);
		MQCFH cfh = new MQCFH(responses[0]);

		if(cfh.reason == 0) {
			MQCFSL cfsl = new MQCFSL(responses[0]);
			for(int i=0;i<cfsl.strings.length;i++) {
				queueList.add(cfsl.strings[i]);
			}
		}
		agent.disconnect();
		return queueList;
	}

	protected Map<String,List<Metric>> reportQueueStatsLite(PCFMessageAgent agent) {
	    Map<String,List<Metric>> metricMap = new HashMap<String, List<Metric>>();

		try {
			logger.debug("Getting queue metrics for queueManager: " + agent.getQManagerName().trim());



			// Prepare PCF command to inquire queue status (status type)
			PCFMessage inquireQueueStatus = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_STATUS);

			inquireQueueStatus.addParameter(MQConstants.MQCA_Q_NAME, "*");
			inquireQueueStatus.addParameter(MQConstants.MQIACF_Q_STATUS_TYPE, MQConstants.MQIACF_Q_STATUS);
			inquireQueueStatus.addParameter(MQConstants.MQIACF_Q_STATUS_ATTRS,
					new int[] { MQConstants.MQCA_Q_NAME, MQConstants.MQIA_CURRENT_Q_DEPTH,
							MQConstants.MQCACF_LAST_GET_DATE, MQConstants.MQCACF_LAST_GET_TIME,
							MQConstants.MQCACF_LAST_PUT_DATE, MQConstants.MQCACF_LAST_PUT_TIME,
							MQConstants.MQIACF_OLDEST_MSG_AGE, MQConstants.MQIA_OPEN_INPUT_COUNT,
							MQConstants.MQIA_OPEN_OUTPUT_COUNT, MQConstants.MQIACF_UNCOMMITTED_MSGS,
							MQConstants.MQIACF_Q_TIME_INDICATOR, MQConstants.MQIACF_Q_STATUS_TYPE});


			PCFMessage[] responses = agent.send(inquireQueueStatus);

			logger.debug("{} queues returned by this query", responses.length);
//			System.out.println("CLASS " + responses[0].getClass());

			int skipCount = 0;
			int reportingCount = 0;
			for (int j = 0; j < responses.length; j++) {
				PCFMessage response = responses[j];
				String qName = response.getStringParameterValue(MQConstants.MQCA_Q_NAME);

				int currentDepth = response.getIntParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH);
				int openInputCount = response.getIntParameterValue(MQConstants.MQIA_OPEN_INPUT_COUNT);
				int openOutputCount = response.getIntParameterValue(MQConstants.MQIA_OPEN_OUTPUT_COUNT);
				int oldestMsgAge = response.getIntParameterValue(MQConstants.MQIACF_OLDEST_MSG_AGE);
				int uncommittedMsgs = response.getIntParameterValue(MQConstants.MQIACF_UNCOMMITTED_MSGS);
				int[] queueTimeIndicator = response.getIntListParameterValue(MQConstants.MQIACF_Q_TIME_INDICATOR);

				String lastGetDate = response.getStringParameterValue(MQConstants.MQCACF_LAST_GET_DATE);
				String lastGetTime = response.getStringParameterValue(MQConstants.MQCACF_LAST_GET_TIME);
				String lastPutDate = response.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_DATE);
				String lastPutTime = response.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_TIME);

				boolean skip = false;
				for (int i = 0; i < queueIgnores.size(); i++) {
					Pattern ignorePattern = queueIgnores.get(i);
					if (ignorePattern.matcher(qName).matches()) {
						skip = true;
						logger.trace("Skipping metrics for queue: {}", qName);
						break;
					}
				}

				if (!skip) {
					reportingCount++;
					if (qName != null) {
						String queueName = qName.trim();
						List<Metric> metricset = new LinkedList<Metric>();
						metricset.add(new AttributeMetric("provider", "IBM"));
						metricset.add(new AttributeMetric("entity", "queue"));
						metricset.add(new AttributeMetric("qManagerName", serverQueueManagerName));
						metricset.add(new AttributeMetric("qManagerHost", this.getServerHost()));
						metricset.add(new AttributeMetric("qName", queueName));
						metricset.add(new GaugeMetric("qDepth", currentDepth));
						metricset.add(new GaugeMetric("openInputCount", openInputCount));
						metricset.add(new GaugeMetric("openOutputCount", openOutputCount));
						metricset.add(new GaugeMetric("oldestMsgAge", oldestMsgAge));
						metricset.add(new GaugeMetric("uncommittedMsgs", uncommittedMsgs));
						if (queueTimeIndicator.length > 0) {
							metricset.add(new GaugeMetric("queueTimeIndicator", queueTimeIndicator[0]));
						}
						metricset.add(new AttributeMetric("lastGet", lastGetDate + " " +lastGetTime));
						metricset.add(new AttributeMetric("lastPut", lastPutDate + " " + lastPutTime));


						metricMap.put(queueName, metricset);

						logger.debug("[queue_name: {}, queue_depth: {}", queueName, currentDepth);
					}
				} else {
					skipCount++;
				}
			}

			logger.debug("{} queues skipped and {} queues reporting for this queue_manager", skipCount, reportingCount);

		} catch (Throwable t) {
			logger.error("Exception occurred", t);
		}
        return metricMap;
	}

    protected Map<String,List<Metric>> reportResetQueueStats(PCFMessageAgent agent,  Map<String,List<Metric>> metricMap) {

        try {

            logger.debug("Getting RestQueueStats metrics for queueManager: " + agent.getQManagerName().trim());

            PCFMessage inquireQueueStatus = new PCFMessage(CMQCFC.MQCMD_RESET_Q_STATS);
            inquireQueueStatus.addParameter(MQConstants.MQCA_Q_NAME, "*");

            PCFMessage[] responses = agent.send(inquireQueueStatus);

            logger.debug("{} queues returned by this query", responses.length);

            int skipCount = 0;
            int reportingCount = 0;
            for (int j = 0; j < responses.length; j++) {
                PCFMessage response = responses[j];
                String qName = response.getStringParameterValue(MQConstants.MQCA_Q_NAME);
                int highQDepth = response.getIntParameterValue(MQConstants.MQIA_HIGH_Q_DEPTH);
                int msgDeqCount = response.getIntParameterValue(MQConstants.MQIA_MSG_DEQ_COUNT);
                int msgEnqCount = response.getIntParameterValue(MQConstants.MQIA_MSG_ENQ_COUNT);

                int timeSinceReset = response.getIntParameterValue(MQConstants.MQIA_TIME_SINCE_RESET);

                boolean skip = false;
                for (int i = 0; i < queueIgnores.size(); i++) {
                    Pattern ignorePattern = queueIgnores.get(i);
                    if (ignorePattern.matcher(qName).matches()) {
                        skip = true;
                        logger.trace("Skipping metrics for queue: {}", qName);
                        break;
                    }
                }

                if (!skip) {
                    reportingCount++;
                    if (qName != null) {
                        String queueName = qName.trim();
                        List<Metric> metricset= metricMap.get(queueName);
                        if (metricset== null){
                            metricset=  new LinkedList<Metric>();
                            metricset.add(new AttributeMetric("provider", "IBM"));
                            metricset.add(new AttributeMetric("entity", "queue"));
                            metricset.add(new AttributeMetric("qManagerName", serverQueueManagerName));
                            metricset.add(new AttributeMetric("qManagerHost", this.getServerHost()));
                            metricset.add(new AttributeMetric("qName", queueName));

                        }

                        metricset.add(new GaugeMetric("highQDepth", highQDepth));
                        metricset.add(new GaugeMetric("msgDeqCount", msgDeqCount));
                        metricset.add(new GaugeMetric("msgEnqCount", msgEnqCount));

                        metricset.add(new GaugeMetric("timeSinceReset", timeSinceReset));
                        metricMap.put(queueName, metricset);

                    }
                } else {
                    skipCount++;
                }
            }

            logger.debug("{} queues skipped and {} queues reporting for this queue_manager", skipCount, reportingCount);
        } catch (Throwable t) {
            logger.error("Exception occurred", t);
        }

        return metricMap;
    }

	protected Map<String,List<Metric>> reportQueueStats(MQQueueManager mqQueueManager) {
        Map<String,List<Metric>> metricMap = new HashMap<String, List<Metric>>();

		try {

			String qMgrName = mqQueueManager.getName().trim();
			logger.debug("Getting queue metrics for queueManager: " + qMgrName);

			List<String> qList = listQueues(mqQueueManager);
			logger.debug("{} queues returned by this query", qList.size() );

			int skipCount = 0;
			int reportingCount = 0;
			for (String qName : qList) {
				boolean skip = false;
				for (int i = 0; i < queueIgnores.size(); i++) {
					Pattern ignorePattern = queueIgnores.get(i);
					if (ignorePattern.matcher(qName).matches()) {
						skip = true;
						logger.trace("Skipping metrics for queue: {}", qName);
						break;
					}
				}

				if (!skip) {
					reportingCount++;
					MQQueue queue = null;
					try {
						int openOptions = MQConstants.MQOO_INQUIRE + MQConstants.MQOO_INPUT_SHARED;
						queue = mqQueueManager.accessQueue(qName, openOptions);
						if (queue.getQueueType() != MQConstants.MQQT_REMOTE) {
							String queueName = queue.getName().trim();
							Integer currentDepth = queue.getCurrentDepth();
							Integer maxDepth = queue.getMaximumDepth();
							Integer openInputCount = queue.getOpenInputCount();
							Integer openOutputCount = queue.getOpenOutputCount();


							Float percent = 0.0F;
							if(maxDepth > 0) {
								percent = 100.0F * currentDepth/maxDepth;
							}
							List<Metric> metricset = new LinkedList<Metric>();
							metricset.add(new AttributeMetric("provider", "IBM"));
							metricset.add(new AttributeMetric("entity", "queue"));
							metricset.add(new AttributeMetric("qManagerName", serverQueueManagerName));
							metricset.add(new AttributeMetric("qManagerHost", this.getServerHost()));
							metricset.add(new AttributeMetric("qName", queueName));
							metricset.add(new GaugeMetric("qDepthPercent", percent));
							metricset.add(new GaugeMetric("qDepth", currentDepth));
							metricset.add(new GaugeMetric("qDepthMax", maxDepth));
							metricset.add(new GaugeMetric("openInputCount", openInputCount));
							metricset.add(new GaugeMetric("openOutputCount", openOutputCount));

                            metricMap.put(queueName, metricset);

                            logger.debug("[queue_name: {}, queue_depth: {}, queue_depth_percent: {}", queueName, currentDepth, percent);
						} else {
							skipCount++;
						}
					} catch (RuntimeException e) {
						logger.error("Failed to get queue statistics for queue: " + qName + ". " + e.getMessage());
					} catch (Exception e) {
						logger.error("Failed to get queue statistics for queue: " + qName + ". " + e.getMessage());
					} finally {
						if (queue != null) {
							queue.close();
						}
					}
				}
			}
			logger.debug("{} queues skipped and {} queues reporting for this queue_manager", skipCount, reportingCount);
		} catch (MQException e) {
			logger.error("MQException occurred", e);
		} catch (IOException e) {
			logger.error("IOException occurred", e);
		} catch (Throwable e) {
			logger.error("Exception occurred", e);
		}
        return metricMap;

	}

	protected void reportChannelStats(PCFMessageAgent agent, MetricReporter metricReporter) {
		int[] attrs = {
				MQConstants.MQCACH_CHANNEL_NAME,
				MQConstants.MQCACH_CONNECTION_NAME,
				MQConstants.MQIACH_CHANNEL_STATUS,
				MQConstants.MQIACH_MSGS,
				MQConstants.MQIACH_BYTES_SENT,
				MQConstants.MQIACH_BYTES_RECEIVED,
				MQConstants.MQIACH_BUFFERS_SENT,
				MQConstants.MQIACH_BUFFERS_RECEIVED };
		try {
			logger.debug("Getting channel metrics for queueManager: ", agent.getQManagerName().trim());

			PCFMessage request = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);
			request.addParameter(MQConstants.MQCACH_CHANNEL_NAME,"*");
			//request.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_TYPE, MQConstants.MQOT_CURRENT_CHANNEL);
			request.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_ATTRS, attrs);
			PCFMessage[] response = agent.send(request);
			for(int i=0;i<response.length;i++) {
				String channelName = response[i].getStringParameterValue(MQConstants.MQCACH_CHANNEL_NAME).trim();

				logger.debug("Reporting metrics on channel: " + channelName);
				PCFMessage msg = response[i];
				int channelStatus = msg.getIntParameterValue(MQConstants.MQIACH_CHANNEL_STATUS);

				int messages = msg.getIntParameterValue(MQConstants.MQIACH_MSGS);

				int bytesSent = msg.getIntParameterValue(MQConstants.MQIACH_BYTES_SENT);

				int bytesRec = msg.getIntParameterValue(MQConstants.MQIACH_BYTES_RCVD);

				int buffersSent = msg.getIntParameterValue(MQConstants.MQIACH_BUFFERS_SENT);

				int buffersRec = msg.getIntParameterValue(MQConstants.MQIACH_BUFFERS_RECEIVED);

				String connectionName = msg.getStringParameterValue(MQConstants.MQCACH_CONNECTION_NAME);


				if (StringUtils.isNotBlank(connectionName)){
					connectionName= connectionName.trim();
				}


				int chType = 0;
				Object channelTypeObj = msg.getParameterValue(CMQCFC.MQIACH_CHANNEL_TYPE);
				if (channelTypeObj != null && channelTypeObj instanceof Integer) {
					chType = ((Integer) channelTypeObj).intValue();
				}


				List<Metric> metricset = new LinkedList<Metric>();
				metricset.add(new AttributeMetric("provider", "ibmMQ"));
				metricset.add(new AttributeMetric("entity", "channel"));

				metricset.add(new AttributeMetric("qManagerName", serverQueueManagerName));
				metricset.add(new AttributeMetric("qManagerHost", this.getServerHost()));
				metricset.add(new AttributeMetric("channelName", channelName));

				String channelTypeStr = channelTypeMap.get(chType);
				metricset.add(new AttributeMetric("channelType", StringUtils.isBlank(channelTypeStr)?"":channelTypeStr));

				String channelStatusStr = channelStatusMap.get(channelStatus);
				metricset.add(new AttributeMetric("channelStatus", StringUtils.isBlank(channelStatusStr)?"UNKNOWN":channelStatusStr));

				metricset.add(new AttributeMetric("connectionName", connectionName));
				metricset.add(new GaugeMetric("messageCount", messages));
				metricset.add(new RateMetric("messageRate", messages));

				metricset.add(new GaugeMetric("bytesSentCount", bytesSent));
				metricset.add(new RateMetric("bytesSentRate", bytesSent));
				metricset.add(new GaugeMetric("bytesRecCount", bytesRec));
				metricset.add(new RateMetric("bytesRecRate", bytesRec));

				metricset.add(new GaugeMetric("buffersSentCount", buffersSent));
				metricset.add(new RateMetric("buffersSentRate", buffersSent));
				metricset.add(new GaugeMetric("bufferRecCount", buffersRec));
				metricset.add(new RateMetric("bufferRecRate", buffersRec));

				logger.debug(
						"[channel_name: {}, channel_status: {}, message_count: {}, bytes_sent: {}, bytes_rec: {}, buffers_sent: {}, buffers_rec: {}",
						channelName, channelStatusStr, messages, bytesSent, bytesRec, buffersSent, buffersRec);
				metricReporter.report(this.getEventType(), metricset, channelName);
			}

		} catch (PCFException e) {
			logger.error("PCFException", e);
		} catch (MQException e) {
			logger.error("MQException", e);
		} catch (IOException e) {
			logger.error("IOException", e);
		} catch (Throwable e) {
			logger.error("IOException", e);
		}
	}

	public void addToQueueIgnores(String queueIgnore) {
		Pattern pattern = Pattern.compile(queueIgnore.trim(), Pattern.CASE_INSENSITIVE);
		queueIgnores.add(pattern);
	}

}