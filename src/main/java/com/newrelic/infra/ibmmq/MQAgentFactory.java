package com.newrelic.infra.ibmmq;

import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.AgentFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MQAgentFactory extends AgentFactory {

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
		//We need to preserve JRE 1.6 compatibility for IBM MQ Monitor. 
		//This is far easier than having our legacy customers install a compatible JRE 1.8 in their environments
		//int version = (Integer) agentProperties.getOrDefault("version", MQAgent.LATEST_VERSION); 
		int version = (int) getOrDefault(agentProperties, "version", MQAgent.LATEST_VERSION);
		
		//boolean reportEventMessages = (Boolean) agentProperties.getOrDefault("reportEventMessages", false);
		boolean reportEventMessages = (Boolean) getOrDefault(agentProperties, "reportEventMessages", false);
		
		//boolean reportMaintenanceErrors = (Boolean) agentProperties.getOrDefault("reportMaintenanceErrors", false);
		boolean reportMaintenanceErrors = (Boolean) getOrDefault(agentProperties, "reportMaintenanceErrors", false);
		
		//String dailyMaintenanceErrorScanTime = (String) agentProperties.getOrDefault("dailyMaintenanceErrorScanTime", null);
		String dailyMaintenanceErrorScanTime = (String) getOrDefault(agentProperties, "dailyMaintenanceErrorScanTime", null);
		
		//String mqToolsLogPath = (String) agentProperties.getOrDefault("mqToolsLogPath", null);
		String mqToolsLogPath = (String) getOrDefault(agentProperties, "mqToolsLogPath", null);
		
		//boolean monitorErrorLogs = (Boolean) agentProperties.getOrDefault("monitorErrorLogs", false);
		boolean monitorErrorLogs = (Boolean) getOrDefault(agentProperties, "monitorErrorLogs", false);
		
		//String errorLogPath = (String) agentProperties.getOrDefault("errorLogPath", null);
		String errorLogPath = (String) getOrDefault(agentProperties, "errorLogPath", null);
		
		//String agentTempPath = (String) agentProperties.getOrDefault("agentTempPath", null);
		String agentTempPath = (String) getOrDefault(agentProperties, "agentTempPath", null);

		if (name == null || host == null || port == null || queueManager == null || channel == null) {
			throw new Exception("'name', 'host', 'port', 'queueManager' and 'channel' are required agent properties.");
		}

		if(reportMaintenanceErrors && (dailyMaintenanceErrorScanTime == null || mqToolsLogPath == null)) {
			throw new Exception("'dailyMaintenanceErrorScanTime' and 'mqToolsLogPath' are required when 'reportMaintenanceErrors' is true");
		}

		if(monitorErrorLogs && errorLogPath == null) {
			throw new Exception("'errorLogPath' is required when 'monitorErrorLogs' is true");
		}

		AgentConfig agentConfig = new AgentConfig();
		agentConfig.setServerHost(host);
		agentConfig.setServerPort(port.intValue());
		agentConfig.setServerAuthUser(username);
		agentConfig.setServerAuthPassword(password);
		agentConfig.setServerChannelName(channel);
		agentConfig.setServerQueueManagerName(queueManager);
		agentConfig.setEventType(eventType);
		agentConfig.setReportEventMessages(reportEventMessages);
		
		agentConfig.setVersion(version);
		agentConfig.setReportMaintenanceErrors(reportMaintenanceErrors);
		agentConfig.setMqToolsLogPath(mqToolsLogPath);
		agentConfig.setMonitorErrorLogs(monitorErrorLogs);
		agentConfig.setErrorLogPath(errorLogPath);
		agentConfig.setAgentTempPath(agentTempPath);

		agentConfig.addToQueueIgnores(globalQueueIgnores);
		agentConfig.addToQueueIncludes(globalQueueIncludes);

		MQAgent agent = new MQAgent(agentConfig, dailyMaintenanceErrorScanTime);
		return agent ;
	}
	
	private static <K, V> V getOrDefault(Map<K,V> map, K key, V defaultValue) {
	    return map.containsKey(key) ? map.get(key) : defaultValue;
	}
}
