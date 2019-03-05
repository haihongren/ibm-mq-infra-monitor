/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.ibmmq;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.CMQXC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.GaugeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;
import com.newrelic.infra.publish.api.metrics.RateMetric;

public class ChannelMetricCollector {

	private static final Logger logger = LoggerFactory.getLogger(ChannelMetricCollector.class);
	
	private static final Map<Integer, String> channelTypeMap;
	private static final Map<Integer, String> channelStatusMap;

	static {
		Map<Integer, String> sChannelStatus = new HashMap<>();
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

		Map<Integer, String> mChannelType = new HashMap<>();
		mChannelType.put(CMQXC.MQCHT_SENDER, "SENDER");
		mChannelType.put(CMQXC.MQCHT_SERVER, "SERVER");
		mChannelType.put(CMQXC.MQCHT_RECEIVER, "RECEIVER");
		mChannelType.put(CMQXC.MQCHT_REQUESTER, "REQUESTER");
		mChannelType.put(CMQXC.MQCHT_CLNTCONN, "CLNTCONN");
		mChannelType.put(CMQXC.MQCHT_SVRCONN, "SVRCONN");
		mChannelType.put(CMQXC.MQCHT_ALL, "ALL");

		channelTypeMap = Collections.unmodifiableMap(mChannelType);
	}
	
	private AgentConfig agentConfig = null;
	
	public ChannelMetricCollector(AgentConfig agentConfig) {
		this.agentConfig  = agentConfig;
	}
	
	public void reportChannelStats(PCFMessageAgent agent, MetricReporter metricReporter) {
			int[] attrs = { MQConstants.MQCACH_CHANNEL_NAME, MQConstants.MQCACH_CONNECTION_NAME,
					MQConstants.MQIACH_CHANNEL_STATUS, MQConstants.MQIACH_MSGS, MQConstants.MQIACH_BYTES_SENT,
					MQConstants.MQIACH_BYTES_RECEIVED, MQConstants.MQIACH_BUFFERS_SENT, MQConstants.MQIACH_BUFFERS_RECEIVED,
					MQConstants.MQIACH_INDOUBT_STATUS };
			try {
				logger.debug("Getting channel metrics for queueManager: ", agentConfig.getServerQueueManagerName().trim());

				PCFMessage request = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);
				request.addParameter(MQConstants.MQCACH_CHANNEL_NAME, "*");
				// request.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_TYPE,
				// MQConstants.MQOT_CURRENT_CHANNEL);
				request.addParameter(MQConstants.MQIACH_CHANNEL_INSTANCE_ATTRS, attrs);
				PCFMessage[] response = agent.send(request);
				for (int i = 0; i < response.length; i++) {
					String channelName = response[i].getStringParameterValue(MQConstants.MQCACH_CHANNEL_NAME).trim();

					logger.debug("Reporting metrics on channel: " + channelName);
					PCFMessage msg = response[i];
					int channelStatus = msg.getIntParameterValue(MQConstants.MQIACH_CHANNEL_STATUS);

					int channelInDoubtStatus = -1;
					try {
						channelInDoubtStatus = msg.getIntParameterValue(MQConstants.MQIACH_INDOUBT_STATUS);
					} catch (PCFException e) {
						// In doubt status is not returned for server connection channels. Log ex if
						// it's some other reason.
						if (e.reasonCode != 3014) {
							throw e;
						}
					}

					int messages = msg.getIntParameterValue(MQConstants.MQIACH_MSGS);

					int bytesSent = msg.getIntParameterValue(MQConstants.MQIACH_BYTES_SENT);

					int bytesRec = msg.getIntParameterValue(MQConstants.MQIACH_BYTES_RCVD);

					int buffersSent = msg.getIntParameterValue(MQConstants.MQIACH_BUFFERS_SENT);

					int buffersRec = msg.getIntParameterValue(MQConstants.MQIACH_BUFFERS_RECEIVED);

					String connectionName = msg.getStringParameterValue(MQConstants.MQCACH_CONNECTION_NAME);

					if (StringUtils.isNotBlank(connectionName)) {
						connectionName = connectionName.trim();
					}

					int chType = 0;
					Object channelTypeObj = msg.getParameterValue(CMQCFC.MQIACH_CHANNEL_TYPE);
					if (channelTypeObj != null && channelTypeObj instanceof Integer) {
						chType = ((Integer) channelTypeObj).intValue();
					}

					List<Metric> metricset = new LinkedList<Metric>();
					metricset.add(new AttributeMetric("provider", "ibm"));
					metricset.add(new AttributeMetric("qManagerName", agentConfig.getServerQueueManagerName()));
					metricset.add(new AttributeMetric("qManagerHost", agentConfig.getServerHost()));

					metricset.add(new AttributeMetric("object", "channel"));
					metricset.add(new AttributeMetric("channelName", channelName));

					String channelTypeStr = channelTypeMap.get(chType);
					metricset.add(
							new AttributeMetric("channelType", StringUtils.isBlank(channelTypeStr) ? "" : channelTypeStr));

					String channelStatusStr = channelInDoubtStatus == MQConstants.MQIACH_INDOUBT_STATUS ? "INDOUBT"
							: channelStatusMap.get(channelStatus);
					metricset.add(new AttributeMetric("channelStatus",
							StringUtils.isBlank(channelStatusStr) ? "UNKNOWN" : channelStatusStr));

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
					metricReporter.report("MQChannelSample", metricset, channelName);
				}

			} catch (PCFException e) {
				logger.error("Error fetching channel metrics", e);
			} catch (IOException e) {
				logger.error("Error fetching channel metrics", e);
			} catch (MQDataException e) {
				logger.error("Error fetching channel metrics", e);
			} 
		}
}
