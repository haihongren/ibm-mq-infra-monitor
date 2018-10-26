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
	
	private List<String> globalQueueIgnores = new LinkedList<String>();
	
	@Override
	public void init(Map<String, Object> globalConfig) {
		super.init(globalConfig);
		Object queueIgnoresObject = globalConfig.get("queueIgnores");
		if (queueIgnoresObject != null) {
			if (queueIgnoresObject instanceof ArrayList) {
				List<String> qIgnores = (ArrayList<String>) queueIgnoresObject;
				for (int i = 0; i < qIgnores.size(); i++) {
					String regEx = qIgnores.get(i);
					globalQueueIgnores.add(regEx);
				}
			}
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
		boolean reportEventMessages = (Boolean) agentProperties.get("reportEventMessages");
		int version = (Integer) agentProperties.getOrDefault("version", MQAgent.LATEST_VERSION);

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
		
		for (Iterator<String> iterator = globalQueueIgnores.iterator(); iterator.hasNext();) {
			String queueIgnoreRegEx = iterator.next();
			agent.addToQueueIgnores(queueIgnoreRegEx);
		}
		return agent ;
	}
}
