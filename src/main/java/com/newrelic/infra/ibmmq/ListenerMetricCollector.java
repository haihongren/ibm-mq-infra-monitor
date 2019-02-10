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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.constants.CMQCFC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;

public class ListenerMetricCollector {

	private static final Logger logger = LoggerFactory.getLogger(ListenerMetricCollector.class);
	
	private AgentConfig agentConfig = null;
	
	public ListenerMetricCollector(AgentConfig agentConfig) {
		this.agentConfig  = agentConfig;
	}
	
	public void reportListenerStatus(PCFMessageAgent agent, MetricReporter metricReporter) {
		try {
			PCFMessage listenerReq = new PCFMessage(MQConstants.MQCMD_INQUIRE_LISTENER);
			listenerReq.addParameter(MQConstants.MQCACH_LISTENER_NAME, "*");

			PCFMessage[] listenerResponses = agent.send(listenerReq);
			for (PCFMessage listenerRes : listenerResponses) {
				String name = listenerRes.getStringParameterValue(MQConstants.MQCACH_LISTENER_NAME);
				if (name.contains(".DEFAULT.")) {
					// Skip the default listener
					continue;
				}

				PCFMessage statusReq = new PCFMessage(CMQCFC.MQCMD_INQUIRE_LISTENER_STATUS);
				statusReq.addParameter(MQConstants.MQCACH_LISTENER_NAME,
						listenerRes.getStringParameterValue(MQConstants.MQCACH_LISTENER_NAME));

				PCFMessage[] statusResponses = agent.send(statusReq);
				for (PCFMessage statusRes : statusResponses) {
					List<Metric> metricset = new LinkedList<>();
					metricset.add(new AttributeMetric("object", "Listener"));
					metricset.add(new AttributeMetric("status", MQAgent.friendlyCodeLookup(
							statusRes.getIntParameterValue(MQConstants.MQIACH_LISTENER_STATUS), "MQSVC_.*")));
					metricset.add(new AttributeMetric("name", name.trim()));

					metricReporter.report(agentConfig.getEventType("SysObjectStatus"), metricset);
				}
			}
		} catch (Exception e) {
			logger.error("Problem getting system object status stats for channel listener.", e);
		}
	}

}
