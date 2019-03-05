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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.ibm.mq.headers.pcf.PCFParameter;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;

public class EventMetricCollector {

	private static final Logger logger = LoggerFactory.getLogger(EventMetricCollector.class);

	private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM HH:mm:ss");
	
	private AgentConfig agentConfig = null;
	
	public EventMetricCollector(AgentConfig agentConfig) {
		this.agentConfig  = agentConfig;
	}
	
	protected void reportEventStats(MQQueueManager mqQueueManager , MetricReporter metricReporter) {
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
			int openOptions = MQConstants.MQOO_INQUIRE + MQConstants.MQOO_FAIL_IF_QUIESCING
					+ MQConstants.MQOO_INPUT_SHARED;

			queue = mgr.accessQueue(queueName, openOptions, null, null, null);

			MQGetMessageOptions getOptions = new MQGetMessageOptions();
			getOptions.options = MQConstants.MQGMO_NO_WAIT + MQConstants.MQGMO_FAIL_IF_QUIESCING
					+ MQConstants.MQGMO_CONVERT;

			MQMessage message = new MQMessage();

			int[] detailsIgnore = new int[] { MQConstants.MQIACF_REASON_QUALIFIER, MQConstants.MQCA_Q_MGR_NAME };
			Arrays.sort(detailsIgnore);

			while (true) {
				try {
					queue.get(message, getOptions);
					PCFMessage pcf = new PCFMessage(message);

					StringBuilder b = new StringBuilder();
					Enumeration params = pcf.getParameters();
					while (params.hasMoreElements()) {
						//TODO: unsafe cast? 
						PCFParameter param = (PCFParameter)params.nextElement();
						if (Arrays.binarySearch(detailsIgnore, param.getParameter()) < 0) {
							b.append(param.getParameterName()).append('=').append(param.getStringValue().trim())
									.append(';');
						}
					}
					String details = b.length() > 0 ? b.substring(0, b.length() - 1) : "";

					List<Metric> metricset = new LinkedList<>();
					metricset.add(new AttributeMetric("provider", "ibm"));
					metricset.add(new AttributeMetric("qManagerName", agentConfig.getServerQueueManagerName()));
					metricset.add(new AttributeMetric("qManagerHost", agentConfig.getServerHost()));
					
					metricset.add(new AttributeMetric("object", "event"));
					metricset.add(new AttributeMetric("putTime", dateTimeFormat.format(message.putDateTime.getTime())));
					metricset.add(new AttributeMetric("eventQueue", queueName));
					metricset.add(new AttributeMetric("queueManager", pcf.getStringParameterValue(MQConstants.MQCA_Q_MGR_NAME).trim()));
					metricset.add(new AttributeMetric("reasonCode", MQConstants.lookupReasonCode(pcf.getReason())));
					metricset.add(new AttributeMetric("reasonQualifier", tryGetPCFIntParam(pcf, MQConstants.MQIACF_REASON_QUALIFIER, "MQRQ_.*")));
					metricset.add(new AttributeMetric("details", details));
					metricReporter.report("MQEventSample", metricset);
					
					message.clearMessage();

				} catch (MQException e) {
					if (e.completionCode == 2 && e.reasonCode == MQConstants.MQRC_NO_MSG_AVAILABLE) {
						// Normal completion with all messages processed.
						break;
					} else {
						throw e;
					}
				} catch (MQDataException e) {
					if (e.completionCode == 2 && e.reasonCode == MQConstants.MQRC_NO_MSG_AVAILABLE) {
						// Normal completion with all messages processed.
						break;
					}
				} 
			}
		} catch (IOException | MQException e) {
			logger.error("Problem getting event stats from " + queueName + ".", e);
		} finally {
			if (queue != null) {
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
			if (e.completionCode == 2 && e.reasonCode == 3014) {
				return "";
			} else {
				throw e;
			}
		}
	}

}
