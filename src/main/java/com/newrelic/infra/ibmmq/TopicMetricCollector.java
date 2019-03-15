/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.ibmmq;

import java.util.Enumeration;
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
import com.ibm.mq.headers.pcf.PCFParameter;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.GaugeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;

import ch.qos.logback.core.net.SyslogOutputStream;

import com.newrelic.infra.ibmmq.constants.QueueSampleConstants;
import com.newrelic.infra.ibmmq.constants.TopicSampleConstants;

public class TopicMetricCollector {
	private static final Logger logger = LoggerFactory.getLogger(TopicMetricCollector.class);
	private AgentConfig agentConfig = null;

	public TopicMetricCollector(AgentConfig config) {
		this.agentConfig  = config;
	}

    public void reportTopicStats(PCFMessageAgent agent, MetricReporter metricReporter) {
		try {
			logger.debug("Getting Topic metrics for queueManager: " + agent.getQManagerName().trim());

			// Prepare PCF command to inquire topic status
			PCFMessage inquireTopic = new PCFMessage(MQConstants.MQCMD_INQUIRE_TOPIC_STATUS); 

			inquireTopic.addParameter(MQConstants.MQCA_TOPIC_STRING, "#");
			// There are three possible values for this input param. Not sure which one(s) to use
			inquireTopic.addParameter(MQConstants.MQIACF_TOPIC_STATUS_TYPE, MQConstants.MQIACF_TOPIC_SUB);
			inquireTopic.addParameter(MQConstants.MQIACF_TOPIC_STATUS_ATTRS,
					new int[] { 
							MQConstants.MQIACF_ALL
						});

			PCFMessage[] responses = agent.send(inquireTopic);

			logger.debug("{} topics returned by this query", responses.length);
			
			int skipCount = 0;
			int reportingCount = 0;
			for (int j = 0; j < responses.length; j++) {
				PCFMessage response = responses[j];
				Enumeration<PCFParameter> responseParams = response.getParameters();
				String topicName = response.getStringParameterValue(MQConstants.MQCA_TOPIC_STRING);
				
				if (!isTopicIgnored(topicName)) {
					reportingCount++;
					if (topicName != null) {
						topicName = topicName.trim();
						List<Metric> metricset = new LinkedList<Metric>();
						addCommonAttribute(metricset, topicName);
						//Do not assume what parameters are returned other than MQCA_TOPIC_STRING. accessing an parameter that does not get returned result in an error
						while (responseParams.hasMoreElements()) {
							PCFParameter param = responseParams.nextElement();
							System.out.println("\t"+param.getParameterName() + " = " + param.getValue() + " // " + param.getValue().getClass().toString());
							if (param.getParameter() == MQConstants.MQIA_DURABLE_SUB) {
								int durable = response.getIntParameterValue(MQConstants.MQIA_DURABLE_SUB);
								metricset.add(new GaugeMetric(EventConstants.DURABLE, durable));
							} else if (param.getParameter() == MQConstants.MQBACF_SUB_ID) {
								byte[] subId = response.getBytesParameterValue(MQConstants.MQBACF_SUB_ID);
								//cannot really report a random byte[], not sure of string conv
								metricset.add(new AttributeMetric(EventConstants.SUB_ID, subId.toString()));
							} else if (param.getParameter() == MQConstants.MQBACF_SUB_ID) {
								String subUserId = response.getStringParameterValue(MQConstants.MQCACF_SUB_USER_ID);
								metricset.add(new AttributeMetric(EventConstants.SUB_USER_ID, new String(subUserId)));
							} else if (param.getParameter() == MQConstants.MQIA_PUB_COUNT) {
								int pubCount = response.getIntParameterValue(MQConstants.MQIA_PUB_COUNT);
								metricset.add(new GaugeMetric(EventConstants.PUB_COUNT, pubCount));
							} else if (param.getParameter() == MQConstants.MQIA_SUB_COUNT) {
								int subCount = response.getIntParameterValue(MQConstants.MQIA_SUB_COUNT);
								metricset.add(new GaugeMetric(EventConstants.SUB_COUNT, subCount));
							}
						}
						metricReporter.report("MQTopicSample", metricset, topicName);
					}
				} else {
					skipCount++;
				}
				

				   
				//byte[] subId = response.getBytesParameterValue(MQConstants.MQBACF_SUB_ID);
				//int currentDepth = response.getIntParameterValue(MQConstants.);
				System.out.println(topicName);

			}

			logger.debug("{} topics skipped and {} topics reporting for this queue_manager", skipCount, reportingCount);

		} catch (Throwable t) {
			logger.error("Exception occurred", t);
		}
	}
    
    private boolean isTopicIgnored(String qName) {
		return false;
	}

	private void addCommonAttribute(List<Metric> metricset, String topicName ){
	    if ((metricset != null && metricset.size() > 0) || StringUtils.isBlank(topicName)){
	        return;
        }

        metricset.add(new AttributeMetric(EventConstants.PROVIDER, EventConstants.IBM_PROVIDER));
        metricset.add(new AttributeMetric(EventConstants.OBJECT_ATTRIBUTE, EventConstants.OBJ_ATTR_TYPE_TOPIC));
        metricset.add(new AttributeMetric(EventConstants.Q_MANAGER_NAME, agentConfig.getServerQueueManagerName()));
        metricset.add(new AttributeMetric(EventConstants.Q_MANAGER_HOST, agentConfig.getServerHost()));
        metricset.add(new AttributeMetric(EventConstants.TOPIC_NAME, topicName.trim()));

    }
}
