package com.newrelic.infra.ibmmq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.AgentFactory;

public class MQMonitorAgentFactory extends AgentFactory {

	private static final int DEFAULT_PORT = 1414;
	
	private ArrayList<String> globalQueueIgnores = new ArrayList<>();
	private ArrayList<String> globalQueueIncludes = new ArrayList<>();
	
	@Override
	public void init(Map<String, Object> globalConfig) {
		super.init(globalConfig);
		loadListFromConfig(globalConfig.get("queueIgnores"), globalQueueIgnores);
		loadListFromConfig(globalConfig.get("queueIncludes"), globalQueueIncludes);
	}

	private void loadListFromConfig(Object configList, List<String> destList) {
		if (configList != null && configList instanceof ArrayList) {
			destList.addAll((ArrayList<String>) configList);
		}
	}
	
	@Override
	public Agent createAgent(Map<String, Object> agentProperties) throws Exception {
		String name = (String) agentProperties.get("name");
		String host = (String) agentProperties.get("host");
		Integer port = (Integer) agentProperties.get("port");
		if (port == null) {
			port = DEFAULT_PORT;
		}
		String username = (String) agentProperties.get("username");
		String password = (String) agentProperties.get("password");
		String queueManager = (String) agentProperties.get("queueManager");
		String channel = (String) agentProperties.get("channel");
		String eventType = (String) agentProperties.get("eventType");
		int version = (Integer) agentProperties.getOrDefault("version", MQAgent.LATEST_VERSION);
		boolean reportEventMessages = (Boolean) agentProperties.get("reportEventMessages");
		boolean reportMaintenanceErrors = (Boolean) agentProperties.get("reportMaintenanceErrors");
		String dailyMaintenanceErrorScanTime = (String) agentProperties.get("dailyMaintenanceErrorScanTime");
		String mqToolsLogPath = (String) agentProperties.get("mqToolsLogPath");

		if (name == null || host == null || port == null || queueManager == null || channel == null) {
			throw new Exception("'name', 'host', 'port', 'queueManager' and 'channel' are required agent properties.");
		}
		
		MQAgent agent = new MQAgent();
		agent.setServerHost(host);
		agent.setServerPort(port.intValue());
		agent.setServerAuthUser(username);
		agent.setServerAuthPassword(password);
		agent.setServerChannelName(channel);
		agent.setServerQueueManagerName(queueManager);
		agent.setEventType(eventType);
		agent.setReportEventMessages(reportEventMessages);
		agent.setVersion(version);
		agent.setReportMaintenanceErrors(reportMaintenanceErrors);
		agent.setDailyMaintenanceErrorScanTime(dailyMaintenanceErrorScanTime);
		agent.setMqToolsLogPath(mqToolsLogPath);

		agent.addToQueueIgnores(globalQueueIgnores);
		agent.addToQueueIncludes(globalQueueIncludes);

		return agent ;
	}
}
