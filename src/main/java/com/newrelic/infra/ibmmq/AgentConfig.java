/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.ibmmq;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class AgentConfig {
	private String serverHost = MQAgent.DEFAULT_SERVER_HOST;
	private int serverPort = MQAgent.DEFAULT_SERVER_PORT;
	private String serverAuthUser = StringUtils.EMPTY;
	private String serverAuthPassword = StringUtils.EMPTY;
	private String serverChannelName = "SYSTEM.DEF.SVRCONN";
	private String serverQueueManagerName = null;
	List<Pattern> queueIgnores = new ArrayList<>();
	List<Pattern> queueIncludes = new ArrayList<>();

	private boolean reportEventMessages = false;
	private boolean reportMaintenanceErrors = false;
	private boolean monitorErrorLogs;
	
	private String mqToolsLogPath;
	private String errorLogPath;
	private String agentTempPath;

	public String getErrorLogPath() {
		return errorLogPath;
	}

	public String getAgentTempPath() {
		return agentTempPath;
	}

	public void setServerHost(String host) {
		this.serverHost = StringUtils.isNotBlank(host) ? host : MQAgent.DEFAULT_SERVER_HOST;

	}

	public String getServerHost() {
		return this.serverHost;

	}

	public void setServerPort(int port) {
		this.serverPort = (port > 0 && port < 65535) ? port : MQAgent.DEFAULT_SERVER_PORT;
		// logger.info("Using Server Port= {} ", this.serverPort);

	}

	public int getServerPort() {
		return this.serverPort;
	}

	public void setServerAuthUser(String serverAuthUser) {
		this.serverAuthUser = serverAuthUser;
	}
	
	public String getServerAuthUser() {
		return serverAuthUser;
	}

	public void setServerAuthPassword(String serverAuthPassword) {
		this.serverAuthPassword = serverAuthPassword;
	}
	
	public String getServerAuthPassword() {
		return serverAuthPassword;
	}

	public void setServerChannelName(String serverChannelName) {
		this.serverChannelName = serverChannelName;
	}
	
	public String getServerChannelName() {
		return serverChannelName;
	}

	public void setServerQueueManagerName(String serverQueueManagerName) {
		this.serverQueueManagerName = serverQueueManagerName;
	}

	public String getServerQueueManagerName() {
		return serverQueueManagerName;
	}
	
	public void setReportEventMessages(boolean reportEventMessages) {
		this.reportEventMessages = reportEventMessages;
	}

	public void setReportMaintenanceErrors(boolean reportMaintenanceErrors) {
		this.reportMaintenanceErrors = reportMaintenanceErrors;
	}

	public void setMqToolsLogPath(String mqToolsLogPath) {
		this.mqToolsLogPath = mqToolsLogPath;
	}
	
	public String getMqToolsLogPath() {
		return mqToolsLogPath;
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
	

	public void addToQueueIgnores(List<String> adds) {
		addPatternsToList(adds, queueIgnores);
	}

	public void addToQueueIncludes(List<String> adds) {
		addPatternsToList(adds, queueIncludes);
	}
	
	private void addPatternsToList(List<String> adds, List<Pattern> list) {
		for (String s : adds) {
			Pattern pattern = Pattern.compile(s.trim(), Pattern.CASE_INSENSITIVE);
			list.add(pattern);
		}
	}

	public boolean reportEventMessages() {
		return reportEventMessages;
	}

	public boolean reportMaintenanceErrors() {
		return reportMaintenanceErrors;
	}

	public boolean monitorErrorLogs() {
		return monitorErrorLogs;
	}
}
