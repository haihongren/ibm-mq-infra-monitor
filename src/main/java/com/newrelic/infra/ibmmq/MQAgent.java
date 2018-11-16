package com.newrelic.infra.ibmmq;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.CMQXC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.*;
import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.InventoryReporter;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.GaugeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;
import com.newrelic.infra.publish.api.metrics.RateMetric;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class MQAgent extends Agent {
	public static final int LATEST_VERSION = 2;

	private static final String QUEUE_ACCESS_PROPERTY = "newrelic.queue.access.allow";
	private static final String DEFAULT_SERVER_HOST = "localhost";
	private static final String DEFAULT_EVENT_TYPE = "IBMMQSample";
	private static final int DEFAULT_SERVER_PORT = 1414;

	private static final Logger logger = LoggerFactory.getLogger(MQAgent.class);

	private boolean accessQueueMode = false;
	private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM HH:mm:ss");
	private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("YYYYMMdd");

	private String serverHost = DEFAULT_SERVER_HOST;
	private int serverPort = DEFAULT_SERVER_PORT;
	private String serverAuthUser = StringUtils.EMPTY;
	private String serverAuthPassword = StringUtils.EMPTY;
	private String serverChannelName = "SYSTEM.DEF.SVRCONN";
	private String serverQueueManagerName = null;
	private String eventType = DEFAULT_EVENT_TYPE;
	private boolean reportEventMessages = false;
	private boolean reportMaintenanceErrors = false;
	private long nextCompressionErrorScanTime;
	private String mqToolsLogPath;
	private boolean monitorErrorLogs;
	private String errorLogPath;
	private String agentTempPath;
	private int version = LATEST_VERSION;

	private MQQueueManager mqQueueManager;
	private PCFMessageAgent agent;
	private MetricReporter metricReporter;

	private List<Pattern> queueIgnores = new ArrayList<>();
	private List<Pattern> queueIncludes = new ArrayList<>();


	private static final Map<Integer,String> channelTypeMap;
	private static final Map<Integer,String> channelStatusMap;
	static{
		Map<Integer,String> sChannelStatus = new HashMap<>();
		sChannelStatus.put(CMQCFC.MQCHS_BINDING, "BINDING");
		sChannelStatus.put(CMQCFC.MQCHS_STARTING, "STARTING");
		sChannelStatus.put(CMQCFC.MQCHS_RUNNING, "RUNNING");
		sChannelStatus.put(CMQCFC.MQCHS_PAUSED, "PAUSED");
		sChannelStatus.put(CMQCFC.MQCHS_STOPPING, "STOPPING");
		sChannelStatus.put(CMQCFC.MQCHS_RETRYING, "RETRYING");
		sChannelStatus.put(CMQCFC.MQCHS_STOPPED, "STOPPED");

		sChannelStatus.put(CMQCFC.MQCHS_REQUESTING, "REQUESTING");
		sChannelStatus.put(CMQCFC.MQCHS_DISCONNECTED, "DISCONNECTED");
		sChannelStatus.put(CMQCFC.MQCHS_INACTIVE, "INACTIVE");
		sChannelStatus.put(CMQCFC.MQCHS_INITIALIZING, "INITIALIZING");
		sChannelStatus.put(CMQCFC.MQCHS_SWITCHING, "SWITCHING");

		channelStatusMap = Collections.unmodifiableMap(sChannelStatus);

		Map<Integer,String> mChannelType = new HashMap<>();
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
		return getEventType("");
	}

	public String getEventType(String subType) {
		if(version > 1 || !(subType.equals("Channel") || subType.equals("Queue")))
			return this.eventType + subType;
		else
			return this.eventType;
	}

	public void setReportEventMessages(boolean reportEventMessages) {
		this.reportEventMessages = reportEventMessages;
	}

	public void setReportMaintenanceErrors(boolean reportMaintenanceErrors) {
		this.reportMaintenanceErrors = reportMaintenanceErrors;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void setDailyMaintenanceErrorScanTime(String time) {
		int index = time.indexOf(':');
		int hour = Integer.parseInt(time.substring(0, index));
		int minute = Integer.parseInt(time.substring(index+1));

		Calendar cal = GregorianCalendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		nextCompressionErrorScanTime = cal.getTimeInMillis();

		if(nextCompressionErrorScanTime < System.currentTimeMillis()) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
			nextCompressionErrorScanTime = cal.getTimeInMillis();
		}
	}

	public void setMqToolsLogPath(String mqToolsLogPath) {
		this.mqToolsLogPath = mqToolsLogPath;
	}

	public void setMonitorErrorLogs(boolean monitorErrorLogs) {
		this.monitorErrorLogs = monitorErrorLogs;
	}

	public void setErrorLogPath(String errorLogPath) {
		this.errorLogPath = errorLogPath;
	}

	public void setAgentTempPath(String agentTempPath) {
		this.agentTempPath = agentTempPath;
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
		this.metricReporter = metricReporter;

        Map<String,List<Metric>> metricMap = new HashMap<>();

        try {
			mqQueueManager = connect();
            agent = new PCFMessageAgent(mqQueueManager);

			if (accessQueueMode) {
                metricMap.putAll(reportQueueStats());
			} else {
				metricMap.putAll(reportQueueStatsLite());
			}
            reportResetQueueStats(metricMap);
			for (Map.Entry<String,List<Metric>> entry : metricMap.entrySet()){
				metricReporter.report(this.getEventType("Queue"), entry.getValue());
			}

            reportChannelStats();

			if(reportEventMessages) {
				reportEventStats();
				reportSysObjectStatusStats();
			}

			if(reportMaintenanceErrors) {
				checkForCompressionError();
			}

			if(monitorErrorLogs) {
				reportErrorLogEvents();
			}

		} catch (MQException e) {
			logger.error("Error occured fetching metrics for {}:{}/{}" , this.getServerHost() , this.getServerPort() , serverQueueManagerName);
			throw e;
		} finally {
            try {
                if (agent !=null){
                    agent.disconnect();
                }
                if (mqQueueManager !=null) {
                    mqQueueManager.disconnect();
                }
            } catch(MQException ex){
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

		List<String> queueList = new ArrayList<>();

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
				queueList.add(cfsl.strings[i].trim());
			}
		}
		agent.disconnect();
		return queueList;
	}

	protected Map<String,List<Metric>> reportQueueStatsLite() {
	    Map<String,List<Metric>> metricMap = new HashMap<>();

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

				if (!isQueueIgnored(qName)) {
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

    protected Map<String,List<Metric>> reportResetQueueStats(Map<String,List<Metric>> metricMap) {

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

                if (!isQueueIgnored(qName)) {
                    reportingCount++;
                    if (qName != null) {
                        String queueName = qName.trim();
                        List<Metric> metricset= metricMap.get(queueName);
                        if (metricset== null){
                            metricset=  new LinkedList<>();
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

    private boolean isQueueIgnored(String qName) {
		for(Pattern includePattern: queueIncludes) {
			if (includePattern.matcher(qName).matches()) {
				return false;
			}
		}

		for (Pattern ignorePattern : queueIgnores) {
			if (ignorePattern.matcher(qName).matches()) {
				logger.trace("Skipping metrics for queue: {}", qName);
				return true;
			}
		}

		return false;
	}

	protected Map<String,List<Metric>> reportQueueStats() {
        Map<String,List<Metric>> metricMap = new HashMap<>();

		try {

			String qMgrName = mqQueueManager.getName().trim();
			logger.debug("Getting queue metrics for queueManager: " + qMgrName);

			List<String> qList = listQueues(mqQueueManager);
			logger.debug("{} queues returned by this query", qList.size() );

			int skipCount = 0;
			int reportingCount = 0;
			for (String qName : qList) {
				if (!isQueueIgnored(qName)) {
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

	protected void reportChannelStats() {
		int[] attrs = {
				MQConstants.MQCACH_CHANNEL_NAME,
				MQConstants.MQCACH_CONNECTION_NAME,
				MQConstants.MQIACH_CHANNEL_STATUS,
				MQConstants.MQIACH_MSGS,
				MQConstants.MQIACH_BYTES_SENT,
				MQConstants.MQIACH_BYTES_RECEIVED,
				MQConstants.MQIACH_BUFFERS_SENT,
				MQConstants.MQIACH_BUFFERS_RECEIVED,
				MQConstants.MQIACH_INDOUBT_STATUS };
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

				int channelInDoubtStatus = -1;
				try {
					channelInDoubtStatus = msg.getIntParameterValue(MQConstants.MQIACH_INDOUBT_STATUS);
				} catch (PCFException e) {
					// In doubt status is not returned for server connection channels.  Log ex if it's some other reason.
					if(e.reasonCode != 3014) {
						throw e;
					}
				}

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

				String channelStatusStr = channelInDoubtStatus == MQConstants.MQIACH_INDOUBT_STATUS ? "INDOUBT" : channelStatusMap.get(channelStatus);
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
				metricReporter.report(this.getEventType("Channel"), metricset, channelName);
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

	protected void reportEventStats() {
		reportEventStatsForQueue(mqQueueManager, metricReporter, "SYSTEM.ADMIN.QMGR.EVENT");
		reportEventStatsForQueue(mqQueueManager, metricReporter, "SYSTEM.ADMIN.CHANNEL.EVENT");
		reportEventStatsForQueue(mqQueueManager, metricReporter, "SYSTEM.ADMIN.PERFM.EVENT");
		reportEventStatsForQueue(mqQueueManager, metricReporter, "SYSTEM.ADMIN.CONFIG.EVENT");
		reportEventStatsForQueue(mqQueueManager, metricReporter, "SYSTEM.ADMIN.COMMAND.EVENT");
		reportEventStatsForQueue(mqQueueManager, metricReporter, "SYSTEM.ADMIN.LOGGER.EVENT");
		reportEventStatsForQueue(mqQueueManager, metricReporter, "SYSTEM.ADMIN.PUBSUB.EVENT");
	}

	protected void reportEventStatsForQueue(MQQueueManager mgr, MetricReporter metricReporter, String queueName) {

		MQQueue queue = null;

		try {
			int openOptions = MQConstants.MQOO_INQUIRE + MQConstants.MQOO_FAIL_IF_QUIESCING + MQConstants.MQOO_INPUT_SHARED;

			queue = mgr.accessQueue(queueName, openOptions, null, null, null);

			MQGetMessageOptions getOptions = new MQGetMessageOptions();
			getOptions.options = MQConstants.MQGMO_NO_WAIT + MQConstants.MQGMO_FAIL_IF_QUIESCING + MQConstants.MQGMO_CONVERT;

			MQMessage message = new MQMessage();

			int[] detailsIgnore = new int[]{MQConstants.MQIACF_REASON_QUALIFIER, MQConstants.MQCA_Q_MGR_NAME};
			Arrays.sort(detailsIgnore);

			while (true) {
				try {
					queue.get(message, getOptions);
					PCFMessage pcf = new PCFMessage(message);

					List<Metric> metricset = new LinkedList<>();

					metricset.add(new AttributeMetric("putTime", dateTimeFormat.format(message.putDateTime.getTime())));
					metricset.add(new AttributeMetric("eventQueue", queueName));
					metricset.add(new AttributeMetric("queueManager", pcf.getStringParameterValue(MQConstants.MQCA_Q_MGR_NAME).trim()));
					metricset.add(new AttributeMetric("reasonCode", MQConstants.lookupReasonCode(pcf.getReason())));
					metricset.add(new AttributeMetric("reasonQualifier", tryGetPCFIntParam(pcf, MQConstants.MQIACF_REASON_QUALIFIER, "MQRQ_.*")));

					StringBuilder b = new StringBuilder();
					Enumeration<PCFParameter> params = pcf.getParameters();
					while (params.hasMoreElements()) {
						PCFParameter param = params.nextElement();
						if (Arrays.binarySearch(detailsIgnore, param.getParameter()) < 0) {
							b.append(param.getParameterName()).append('=').append(param.getStringValue().trim()).append(';');
						}
					}
					String details = b.length() > 0 ? b.substring(0, b.length() - 1) : "";
					metricset.add(new AttributeMetric("details", details));

					metricReporter.report(this.getEventType("Event"), metricset);

					message.clearMessage();

				} catch (MQException e) {
					if (e.completionCode == 2 && e.reasonCode == MQConstants.MQRC_NO_MSG_AVAILABLE) {
						// Normal completion with all messages processed.
						break;
					} else {
						throw e;
					}
				}
			}
		} catch (IOException|MQException e) {
			logger.error("Problem getting event stats from " + queueName + ".", e);
		} finally {
			if(queue != null) {
				try {
					queue.close();
				} catch (MQException e) {
					logger.error("Couldn't close queue " + queueName);
				}
			}
		}
	}

	private String tryGetPCFIntParam(PCFMessage pcf, int paramId, String lookupFilter) throws PCFException {
		try {
			return MQConstants.lookup(pcf.getIntParameterValue(paramId), lookupFilter);
		} catch (PCFException e) {
			if(e.completionCode == 2 && e.reasonCode == 3014) {
				return "";
			} else {
				throw e;
			}
		}
	}

	protected void reportSysObjectStatusStats() {
		reportChannelInitiatorStatus();
		reportClusterQueueManagerSuspended();
		reportChannelListenerStatus();
	}

	private void reportChannelInitiatorStatus() {
		try {
			PCFMessage req = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_MGR_STATUS);
			req.addParameter(MQConstants.MQIACF_Q_MGR_STATUS_ATTRS, new int[]{
					MQConstants.MQIACF_CHINIT_STATUS
			});

			agent.connect(mqQueueManager);

			PCFMessage[] responses = agent.send(req);
			for (PCFMessage res : responses) {
				List<Metric> metricset = new LinkedList<>();
				metricset.add(new AttributeMetric("object", "QueueManagerChannelInitiator"));
				metricset.add(new AttributeMetric("status", friendlyCodeLookup(
						res.getIntParameterValue(MQConstants.MQIACF_CHINIT_STATUS), "MQSVC_.*")));
				metricset.add(new AttributeMetric("name", res.getStringParameterValue(MQConstants.MQCA_Q_MGR_NAME).trim()));
				sendSysObjectStatusMetrics(metricset);
			}
		} catch (MQException|IOException e) {
			logger.error("Problem getting system object status stats for queue manager channel initiator.", e);
		}
	}

	private void reportClusterQueueManagerSuspended() {
		try {
			PCFMessage req = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CLUSTER_Q_MGR);
			req.addParameter(MQConstants.MQCA_CLUSTER_Q_MGR_NAME, "*");
			req.addParameter(MQConstants.MQIACF_CLUSTER_Q_MGR_ATTRS, new int[]{
					MQConstants.MQIACF_SUSPEND
			});

			agent.connect(mqQueueManager);

			PCFMessage[] responses = agent.send(req);
			for (PCFMessage res : responses) {
				List<Metric> metricset = new LinkedList<>();
				metricset.add(new AttributeMetric("object", "ClusterQueueManager"));
				metricset.add(new AttributeMetric("name", res.getStringParameterValue(MQConstants.MQCA_Q_MGR_NAME)));

				int suspended = res.getIntParameterValue(MQConstants.MQIACF_SUSPEND);
				metricset.add(new AttributeMetric("status", suspended == MQConstants.MQSUS_YES ? "SUSPENDED" : ""));

				sendSysObjectStatusMetrics(metricset);
			}
		} catch (Exception e) {
			if(e instanceof PCFException && ((PCFException)e).reasonCode == 2085) {
				logger.debug("No cluster queue manager configured to check if suspended.");
			} else {
				logger.error("Problem getting system object status stats for cluster queue manager.", e);
			}
		}
	}

	private void reportChannelListenerStatus() {
		try {
			PCFMessage listenerReq = new PCFMessage(CMQCFC.MQCMD_INQUIRE_LISTENER);
			listenerReq.addParameter(MQConstants.MQCACH_LISTENER_NAME, "*");
			agent.connect(mqQueueManager);

			PCFMessage[] listenerResponses = agent.send(listenerReq);
			for (PCFMessage listenerRes : listenerResponses) {
				String name = listenerRes.getStringParameterValue(MQConstants.MQCACH_LISTENER_NAME);
				if(name.contains(".DEFAULT.")) {
					// Skip the default listener
					continue;
				}

				PCFMessage statusReq = new PCFMessage(CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS);
				statusReq.addParameter(MQConstants.MQCACH_LISTENER_NAME,
						listenerRes.getStringParameterValue(MQConstants.MQCACH_LISTENER_NAME));
				agent.connect(mqQueueManager);

				PCFMessage[] statusResponses = agent.send(statusReq);
				for (PCFMessage statusRes : statusResponses) {
					List<Metric> metricset = new LinkedList<>();
					metricset.add(new AttributeMetric("object", "ChannelListener"));
					metricset.add(new AttributeMetric("status", friendlyCodeLookup(
							statusRes.getIntParameterValue(MQConstants.MQIACH_LISTENER_STATUS), "MQSVC_.*")));
					metricset.add(new AttributeMetric("name", name.trim()));

					sendSysObjectStatusMetrics(metricset);
				}
			}
		} catch (Exception e) {
			logger.error("Problem getting system object status stats for channel listener.", e);
		}
	}

	private void checkForCompressionError() {
		long now = System.currentTimeMillis();
		if(now >= nextCompressionErrorScanTime) {
			System.out.println("***** checking for compression error");

			String fileDate = fileNameDateFormat.format(new Date(now));
			File logDir = new File(mqToolsLogPath);
			File file = new File(logDir, "mqmaint_err." + fileDate + ".log");

			if(file.exists()) {
				try (BufferedReader in = new BufferedReader(new FileReader(file))) {
					String line;
					while((line = in.readLine()) != null) {
						if(line.contains("Compressing")) {
							List<Metric> metricset = new LinkedList<>();
							metricset.add(new AttributeMetric("queueManager", mqQueueManager.getName()));
							metricset.add(new AttributeMetric("reasonCode", "COMPRESSING_ERROR"));
							metricReporter.report(getEventType("Event"), metricset);

							break;
						}
					}
				} catch (IOException|MQException e) {
					logger.error("Trouble trying to scan for compression error in mqtools logs.", e);
				}
			}

			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(nextCompressionErrorScanTime);
			cal.add(Calendar.DAY_OF_YEAR, 1);
			nextCompressionErrorScanTime = cal.getTimeInMillis();
		}
	}

	private void reportErrorLogEvents() {
		String filePath = errorLogPath + "/AMQERR01.LOG";
		LogReader log = new LogReader(filePath, agentTempPath + "/log-reader.state", "AMQ9526");

		try {
			String line = log.findSearchValueLine();
			if (line != null) {
				List<Metric> metricset = new LinkedList<>();
				metricset.add(new AttributeMetric("queueManager", mqQueueManager.getName()));
				metricset.add(new AttributeMetric("reasonCode", "CHANNEL_OUT_OF_SYNC"));
				metricset.add(new AttributeMetric("details", line));
				metricReporter.report(getEventType("Event"), metricset);
			}
		} catch (IOException|MQException e) {
			logger.error("Trouble searching " + filePath + " for errors.");
		}
	}

	private void sendSysObjectStatusMetrics(List<Metric> metricset) {
		metricReporter.report(this.getEventType("SysObjectStatus"), metricset);
	}

	// Often times a code lookup will result in a lengthy description like abc/xyz/someValue and we just want someValue.
	public String friendlyCodeLookup(int code, String filter) {
		String desc = MQConstants.lookup(code, filter);
		int index = desc.lastIndexOf('/');
		return index == -1 ? desc : desc.substring(index + 1);
	}

	public void addToQueueIgnores(List<String> adds) {
		addPatternsToList(adds, queueIgnores);
	}

	public void addToQueueIncludes(List<String> adds) {
		addPatternsToList(adds, queueIncludes);
	}

	private void addPatternsToList(List<String> adds, List<Pattern> list) {
		for(String s: adds) {
			Pattern pattern = Pattern.compile(s.trim(), Pattern.CASE_INSENSITIVE);
			list.add(pattern);
		}
	}

}