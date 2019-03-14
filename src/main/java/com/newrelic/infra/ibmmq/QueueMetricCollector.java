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

import com.newrelic.infra.ibmmq.constants.EventConstants;
import org.apache.commons.lang3.StringUtils;
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
import com.newrelic.infra.ibmmq.constants.QueueSampleConstants;

public class QueueMetricCollector {
	private static final Logger logger = LoggerFactory.getLogger(QueueMetricCollector.class);
	private AgentConfig agentConfig = null;

	public QueueMetricCollector(AgentConfig config) {
		this.agentConfig  = config;
	}
	

    public void reportQueueStatusStats(PCFMessageAgent agent, MetricReporter metricReporter, Map<String, List<Metric>> metricMap) {
        try {
            logger.debug("Getting Queue Status metrics for queueManager: " + agent.getQManagerName().trim());

            // Prepare PCF command to inquire queue status (status type)
            PCFMessage inquireQueueStatus = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q_STATUS);

            inquireQueueStatus.addParameter(MQConstants.MQCA_Q_NAME, "*");
            inquireQueueStatus.addParameter(MQConstants.MQIACF_Q_STATUS_TYPE, MQConstants.MQIACF_Q_STATUS);
            inquireQueueStatus.addParameter(MQConstants.MQIACF_Q_STATUS_ATTRS,
                    new int[] {
                            MQConstants.MQIACF_OLDEST_MSG_AGE,
                            MQConstants.MQIACF_UNCOMMITTED_MSGS,
                            MQConstants.MQCACF_LAST_GET_DATE, MQConstants.MQCACF_LAST_GET_TIME,
                            MQConstants.MQCACF_LAST_PUT_DATE, MQConstants.MQCACF_LAST_PUT_TIME
                    });

            PCFMessage[] responses = agent.send(inquireQueueStatus);

            logger.debug("{} Queue Status returned by this query", responses.length);

            int skipCount = 0;
            int reportingCount = 0;

            for (int j = 0; j < responses.length; j++) {
                PCFMessage response = responses[j];
                String qName = response.getStringParameterValue(MQConstants.MQCA_Q_NAME);

                int oldestMsgAge = response.getIntParameterValue(MQConstants.MQIACF_OLDEST_MSG_AGE);
                int uncommittedMsgs = response.getIntParameterValue(MQConstants.MQIACF_UNCOMMITTED_MSGS);
                String lastGetDate = response.getStringParameterValue(MQConstants.MQCACF_LAST_GET_DATE);
                String lastGetTime = response.getStringParameterValue(MQConstants.MQCACF_LAST_GET_TIME);
                String lastPutDate = response.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_DATE);
                String lastPutTime = response.getStringParameterValue(MQConstants.MQCACF_LAST_PUT_TIME);


                if (isQueueIgnored(qName)) {
                    skipCount++;
                    continue;
                }

                reportingCount++;
                String queueName = qName.trim();

                List<Metric> metricset = metricMap.get(queueName);
                addCommonAttribute(metricset, queueName);


                metricset.add(new GaugeMetric(QueueSampleConstants.OLDEST_MSG_AGE, oldestMsgAge));
                metricset.add(new GaugeMetric(QueueSampleConstants.UNCOMITTED_MSGS, uncommittedMsgs));

                metricset.add(new AttributeMetric(QueueSampleConstants.LAST_GET_DATE_TIME, String.format("%s %s",lastGetDate ,lastGetTime).trim()));
                metricset.add(new AttributeMetric(QueueSampleConstants.LAST_PUT_DATE_TIME, String.format("%s %s",lastPutDate ,lastPutTime).trim()));
                metricMap.put(queueName, metricset);

            }

            logger.debug("{} queues skipped and {} queues reporting for this queue_manager", skipCount, reportingCount);

        } catch (Throwable t) {
            logger.error("Exception occurred", t);
        }
    }

    public void reportQueueStats(PCFMessageAgent agent, MetricReporter metricReporter, Map<String, List<Metric>> metricMap) {
		try {
			logger.debug("Getting Queue metrics for queueManager: " + agent.getQManagerName().trim());

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

                int qDepthPercent =  (maxDepth>0)? (currentDepth*100 / maxDepth  ) : 0 ;

				if (!isQueueIgnored(qName)) {
					reportingCount++;
					if (qName != null) {
						String queueName = qName.trim();
						List<Metric> metricset = new LinkedList<Metric>();
						addCommonAttribute(metricset, queueName);

						metricset.add(new GaugeMetric(QueueSampleConstants.Q_DEPTH, currentDepth));
						metricset.add(new GaugeMetric(QueueSampleConstants.Q_MAX_DEPTH, maxDepth));
						metricset.add(new GaugeMetric(QueueSampleConstants.OPEN_INPUT_COUNT, openInputCount));
						metricset.add(new GaugeMetric(QueueSampleConstants.OPEN_OUTPUT_COUNT, openOutputCount));

                        metricset.add(new GaugeMetric(QueueSampleConstants.Q_DEPTH_PERCENT, qDepthPercent));
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
                        addCommonAttribute(metricset, queueName);

                        metricset.add(new GaugeMetric(QueueSampleConstants.HIGH_Q_DEPTH, highQDepth));
						metricset.add(new GaugeMetric(QueueSampleConstants.MSG_DEQ_COUNT, msgDeqCount));
						metricset.add(new GaugeMetric(QueueSampleConstants.MSG_ENQ_COUNT, msgEnqCount));

						metricset.add(new GaugeMetric(QueueSampleConstants.TIME_SINCE_RESET, timeSinceReset));
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
	    if (StringUtils.isBlank(qName)){
	        return true;
        }

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



    private void addCommonAttribute(List<Metric> metricset, String queueName ){
	    if ((metricset != null && metricset.size() > 0) || StringUtils.isBlank(queueName)){
	        return;
        }

        metricset.add(new AttributeMetric(EventConstants.PROVIDER, EventConstants.IBM_PROVIDER));
        metricset.add(new AttributeMetric(EventConstants.OBJECT_ATTRIBUTE, EventConstants.OBJ_ATTR_TYPE_QUEUE));
        metricset.add(new AttributeMetric(EventConstants.Q_MANAGER_NAME, agentConfig.getServerQueueManagerName()));
        metricset.add(new AttributeMetric(EventConstants.Q_MANAGER_HOST, agentConfig.getServerHost()));
        metricset.add(new AttributeMetric(EventConstants.Q_NAME, queueName.trim()));

    }
}
