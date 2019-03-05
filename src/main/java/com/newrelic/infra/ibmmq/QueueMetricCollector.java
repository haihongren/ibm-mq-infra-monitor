/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.ibmmq;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.GaugeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;

public class QueueMetricCollector {
	private static final Logger logger = LoggerFactory.getLogger(QueueMetricCollector.class);
	private AgentConfig agentConfig = null;
	
	public QueueMetricCollector(AgentConfig config) {
		this.agentConfig  = config;
	}
	
	//TODO collect some metrics exposed by MQCMD_INQUIRE_Q_STATUS
	
	public void reportQueueStats(PCFMessageAgent agent, MetricReporter metricReporter, Map<String, List<Metric>> metricMap) {
		try {
			logger.debug("Getting queue metrics for queueManager: " + agent.getQManagerName().trim());

			// Prepare PCF command to inquire queue status (status type) 
			PCFMessage inquireQueue = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q); 

			inquireQueue.addParameter(MQConstants.MQCA_Q_NAME, "*");
			inquireQueue.addParameter(MQConstants.MQIA_Q_TYPE, MQConstants.MQQT_LOCAL);
			inquireQueue.addParameter(MQConstants.MQIACF_Q_ATTRS,
					new int[] { 
							MQConstants.MQIA_CURRENT_Q_DEPTH,
							MQConstants.MQIA_MAX_Q_DEPTH,
							MQConstants.MQIA_OPEN_INPUT_COUNT,
							MQConstants.MQIA_OPEN_OUTPUT_COUNT, 
							MQConstants.MQIA_Q_TYPE
						});

			PCFMessage[] responses = agent.send(inquireQueue);

			logger.debug("{} queues returned by this query", responses.length);
			
			int skipCount = 0;
			int reportingCount = 0;
			for (int j = 0; j < responses.length; j++) {
				PCFMessage response = responses[j];
				String qName = response.getStringParameterValue(MQConstants.MQCA_Q_NAME);

				int currentDepth = response.getIntParameterValue(MQConstants.MQIA_CURRENT_Q_DEPTH);
				int maxDepth = response.getIntParameterValue(MQConstants.MQIA_MAX_Q_DEPTH);
				int openInputCount = response.getIntParameterValue(MQConstants.MQIA_OPEN_INPUT_COUNT);
				int openOutputCount = response.getIntParameterValue(MQConstants.MQIA_OPEN_OUTPUT_COUNT);
				//int qTyp = response.getIntParameterValue(MQConstants.MQIA_Q_TYPE);
				
				if (!isQueueIgnored(qName)) {
					reportingCount++;
					if (qName != null) {
						String queueName = qName.trim();
						List<Metric> metricset = new LinkedList<Metric>();
						metricset.add(new AttributeMetric("provider", "ibm"));
						metricset.add(new AttributeMetric("qManagerName", agentConfig.getServerQueueManagerName()));
						metricset.add(new AttributeMetric("qManagerHost", agentConfig.getServerHost()));
						
						metricset.add(new AttributeMetric("object", "queue"));
						metricset.add(new AttributeMetric("qName", queueName));
						metricset.add(new GaugeMetric("qDepth", currentDepth));
						metricset.add(new GaugeMetric("qDepthMax", maxDepth));
						metricset.add(new GaugeMetric("openInputCount", openInputCount));
						metricset.add(new GaugeMetric("openOutputCount", openOutputCount));

						if (maxDepth != 0) {
							metricset.add(new GaugeMetric("qDepthPercent", (currentDepth * 100 / maxDepth) ));
						}
						metricMap.put(queueName, metricset);
						logger.debug("[queue_name: {}, queue_depth: {}]", queueName, currentDepth);
					}
				} else {
					skipCount++;
				}
			}

			logger.debug("{} queues skipped and {} queues reporting for this queue_manager", skipCount, reportingCount);

		} catch (Throwable t) {
			logger.error("Exception occurred", t);
		}
	}

	public void reportResetQueueStats(PCFMessageAgent agent, MetricReporter metricReporter, Map<String, List<Metric>> metricMap) {
		try {

			logger.debug("Getting ResetQueueStats metrics for queueManager: " + agentConfig.getServerQueueManagerName());

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
						List<Metric> metricset = metricMap.get(queueName);
						if (metricset == null) {
							metricset = new LinkedList<>();
							metricset.add(new AttributeMetric("provider", "IBM"));
							metricset.add(new AttributeMetric("entity", "queue"));
							metricset.add(new AttributeMetric("qManagerName", agentConfig.getServerQueueManagerName()));
							metricset.add(new AttributeMetric("qManagerHost", agentConfig.getServerHost()));
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
	}
	

	private boolean isQueueIgnored(String qName) {
		for (Pattern includePattern : agentConfig.queueIncludes) {
			if (includePattern.matcher(qName).matches()) {
				return false;
			}
		}

		for (Pattern ignorePattern : agentConfig.queueIgnores) {
			if (ignorePattern.matcher(qName).matches()) {
				logger.trace("Skipping metrics for queue: {}", qName);
				return true;
			}
		}

		return false;
	}
}
