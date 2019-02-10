/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.ibmmq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;

public class LogMetricCollector {
	private static final Logger logger = LoggerFactory.getLogger(LogMetricCollector.class);

	private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("YYYYMMdd");
	
	private AgentConfig agentConfig = null;
	
	private long nextCompressionErrorScanTime;

	public LogMetricCollector(AgentConfig agentConfig) {
		this.agentConfig  = agentConfig;
	}

	public void reportErrorLogEvents(MQQueueManager mqQueueManager, MetricReporter metricReporter) {
		String filePath = agentConfig.getErrorLogPath() + "/AMQERR01.LOG";
		LogReader log = new LogReader(filePath, agentConfig.getAgentTempPath() + "/log-reader.state", "AMQ9526");

		try {
			String line = log.findSearchValueLine();
			if (line != null) {
				List<Metric> metricset = new LinkedList<>();
				metricset.add(new AttributeMetric("queueManager", mqQueueManager.getName()));
				metricset.add(new AttributeMetric("reasonCode", "CHANNEL_OUT_OF_SYNC"));
				metricset.add(new AttributeMetric("details", line));
				metricReporter.report(agentConfig.getEventType("Event"), metricset);
			}
		} catch (IOException | MQException e) {
			logger.error("Trouble searching " + filePath + " for errors.");
		}
	}

	public void setDailyMaintenanceErrorScanTime(String time) {
		if (!agentConfig.reportMaintenanceErrors()) {
			//logger.debug("Skipped setDailyMaintenanceErrorScanTime={}", agentConfig.reportMaintenanceErrors());
			return;
		}

		int index = time.indexOf(':');
		int hour = Integer.parseInt(time.substring(0, index));
		int minute = Integer.parseInt(time.substring(index + 1));

		Calendar cal = GregorianCalendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		nextCompressionErrorScanTime = cal.getTimeInMillis();

		if (nextCompressionErrorScanTime < System.currentTimeMillis()) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
			nextCompressionErrorScanTime = cal.getTimeInMillis();
		}
	}
	
	public void checkForCompressionError(MQQueueManager mqQueueManager, MetricReporter metricReporter) {
		long now = System.currentTimeMillis();
		if (now >= nextCompressionErrorScanTime) {
			//System.out.println("***** checking for compression error");

			String fileDate = fileNameDateFormat.format(new Date(now));
			File logDir = new File(agentConfig.getMqToolsLogPath());
			File file = new File(logDir, "mqmaint_err." + fileDate + ".log");

			if (file.exists()) {
				try (BufferedReader in = new BufferedReader(new FileReader(file))) {
					String line;
					while ((line = in.readLine()) != null) {
						if (line.contains("Compressing")) {
							List<Metric> metricset = new LinkedList<>();
							metricset.add(new AttributeMetric("queueManager", mqQueueManager.getName()));
							metricset.add(new AttributeMetric("reasonCode", "COMPRESSING_ERROR"));
							metricReporter.report(agentConfig.getEventType("Event"), metricset);

							break;
						}
					}
				} catch (IOException | MQException e) {
					logger.error("Trouble trying to scan for compression error in mqtools logs.", e);
				}
			}

			Calendar cal = GregorianCalendar.getInstance();
			cal.setTimeInMillis(nextCompressionErrorScanTime);
			cal.add(Calendar.DAY_OF_YEAR, 1);
			nextCompressionErrorScanTime = cal.getTimeInMillis();
		}
	}

}
