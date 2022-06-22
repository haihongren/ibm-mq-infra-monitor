package com.newrelic.infra.ibmmq;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.AgentFactory;

public class MQAgentFactory extends AgentFactory {

	private static final int DEFAULT_PORT = 1414;
	
	private ArrayList<String> globalQueueIgnores = new ArrayList<>();
	private ArrayList<String> globalQueueIncludes = new ArrayList<>();
	private ArrayList<String> globalTopicIgnores = new ArrayList<>();
	private ArrayList<String> globalTopicIncludes = new ArrayList<>();
	private ArrayList<String> globalQueueToPolls = new ArrayList<>();
	
	private static final Logger logger = LoggerFactory.getLogger(MQAgentFactory.class);
	@Override
	public void init(Map<String, Object> globalConfig) {
		super.init(globalConfig);
		loadListFromConfig(globalConfig.get("queueIgnores"), globalQueueIgnores);
		loadListFromConfig(globalConfig.get("queueIncludes"), globalQueueIncludes);
		loadListFromConfig(globalConfig.get("topicIgnores"), globalTopicIgnores);
		loadListFromConfig(globalConfig.get("topicIncludes"), globalTopicIncludes);
		loadListFromConfig(globalConfig.get("queueToPolls"), globalQueueToPolls);		
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
//		add cipherSuite for SSL support hren
		String cipherSuite = (String) getOrDefault(agentProperties, "cipherSuite", "");
		String modelQueue = (String) getOrDefault(agentProperties, "modelQueue", "");
		String commandQueue = (String) getOrDefault(agentProperties, "commandQueue", "");
		
		String sslTrustStore = (String) getOrDefault(agentProperties, "sslTrustStore", "");
		String sslTrustStorePassword = (String) getOrDefault(agentProperties, "sslTrustStorePassword", "");
		String sslKeyStore = (String) getOrDefault(agentProperties, "sslKeyStore", "");
		String sslKeyStorePassword = (String) getOrDefault(agentProperties, "sslKeyStorePassword", "");

//		add queueToPolls support for agent instance
		ArrayList<String> localQueueToPolls = new ArrayList<>();
		Object queueToPolls = (Object) getOrDefault(agentProperties, "queueToPolls", new ArrayList<String>());
		this.loadListFromConfig(queueToPolls,localQueueToPolls);
//		add queueIgnores support for agent instance
		ArrayList<String> localQueueIgnores = new ArrayList<>();
		Object queueIgnores = (Object) getOrDefault(agentProperties, "queueIgnores", new ArrayList<String>());
		this.loadListFromConfig(queueIgnores,localQueueIgnores);

//		add queueIncludes support for agent instance
		ArrayList<String> localQueueIncludes= new ArrayList<>();
		Object queueIncludes = (Object) getOrDefault(agentProperties, "queueIncludes", new ArrayList<String>());
		this.loadListFromConfig(queueIncludes,localQueueIncludes);

//		add topicIgnores support for agent instance
		ArrayList<String> localTopicIgnores = new ArrayList<>();
		Object topicIgnores = (Object) getOrDefault(agentProperties, "topicIgnores", new ArrayList<String>());
		this.loadListFromConfig(topicIgnores,localTopicIgnores);

//		add topicIncludes support for agent instance
		ArrayList<String> localTopicIncludes= new ArrayList<>();
		Object topicIncludes = (Object) getOrDefault(agentProperties, "topicIncludes", new ArrayList<String>());
		this.loadListFromConfig(topicIncludes,localTopicIncludes);


		if (port == null) {
			port = DEFAULT_PORT;
		}
		String username = (String) agentProperties.get("username");
		String password = (String) agentProperties.get("password");
		String queueManager = (String) agentProperties.get("queueManager");
		String channel = (String) agentProperties.get("channel");
		//We need to preserve JRE 1.6 compatibility for IBM MQ Monitor. 
		//This is far easier than having our legacy customers install a compatible JRE 1.8 in their environments
		//int version = (Integer) agentProperties.getOrDefault("version", MQAgent.LATEST_VERSION); 
		//int version = (int) getOrDefault(agentProperties, "version", MQAgent.LATEST_VERSION);
		
		//boolean reportEventMessages = (Boolean) agentProperties.getOrDefault("reportEventMessages", false);
		boolean reportEventMessages = (Boolean) getOrDefault(agentProperties, "reportEventMessages", false);
		boolean reportAdditionalQueueStatus =  (Boolean) getOrDefault(agentProperties, "reportAdditionalQueueStatus", false);
		boolean reportTopicStatus =  (Boolean) getOrDefault(agentProperties, "reportTopicStatus", false);
		boolean reportAdditionalTopicStatus =  (Boolean) getOrDefault(agentProperties, "reportAdditionalTopicStatus", false);

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
		agentConfig.setReportEventMessages(reportEventMessages);
		agentConfig.setReportMaintenanceErrors(reportMaintenanceErrors);
		agentConfig.setMqToolsLogPath(mqToolsLogPath);
		agentConfig.setMonitorErrorLogs(monitorErrorLogs);
		agentConfig.setErrorLogPath(errorLogPath);
		agentConfig.setAgentTempPath(agentTempPath);
		agentConfig.setReportAdditionalQueueStatus(reportAdditionalQueueStatus);
		agentConfig.setReportTopicStatus(reportTopicStatus);
		agentConfig.setReportAdditionalTopicStatus(reportAdditionalTopicStatus);

//		add cipherSuite for SSL support hren
		agentConfig.setcipherSuite(cipherSuite);
		agentConfig.setModelQueue(modelQueue);
		agentConfig.setCommandQueue(commandQueue);
		
		agentConfig.setSslTrustStore(sslTrustStore);
		agentConfig.setSslTrustStorePassword(sslTrustStorePassword);
		agentConfig.setSslKeyStore(sslKeyStore);
		agentConfig.setSslKeyStorePassword(sslKeyStorePassword);

		if (localQueueToPolls.isEmpty()) {
			agentConfig.addToQueueToPolls(globalQueueToPolls);
		}else{
			agentConfig.addToQueueToPolls(localQueueToPolls);
		}

		if (localQueueIgnores.isEmpty()) {
			agentConfig.addToQueueIgnores(globalQueueIgnores);
		}else{
			agentConfig.addToQueueIgnores(localQueueIgnores);
		}

		if (localQueueIncludes.isEmpty()) {
			agentConfig.addToQueueIncludes(globalQueueIncludes);
		}else{
			agentConfig.addToQueueIncludes(localQueueIncludes);
		}

		if (localTopicIgnores.isEmpty()) {
			agentConfig.addToTopicIgnores(globalTopicIgnores);
		}else{
			agentConfig.addToTopicIgnores(localTopicIgnores);
		}

		if (localTopicIncludes.isEmpty()) {
			agentConfig.addToTopicIncludes(globalTopicIncludes);
		}else{
			agentConfig.addToTopicIncludes(localTopicIncludes);
		}

		MQAgent agent = new MQAgent(agentConfig, dailyMaintenanceErrorScanTime);
		return agent ;
	}
	
	private static <K, V> V getOrDefault(Map<K,V> map, K key, V defaultValue) {
	    return map.containsKey(key) ? map.get(key) : defaultValue;
	}
}
