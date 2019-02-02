package com.newrelic.infra.ibmmq;

import org.junit.Test;

import static org.junit.Assert.*;

public class MQAgentTest {
    @Test
    public void testSetServerQueueManagerName() {
    }

    @Test
    public void testSetServerHost() {
        final String DefaultServerHost = "localhost";
        AgentConfig agentConfig = new AgentConfig();

        // Assert the default comes through
        assertEquals(DefaultServerHost, agentConfig.getServerHost());

        // Setting it Null returns default
        agentConfig.setServerHost(null);
        assertEquals(DefaultServerHost, agentConfig.getServerHost());

        // Setting it Empty returns default
        agentConfig.setServerHost("");
        assertEquals(DefaultServerHost, agentConfig.getServerHost());

        // Setting it to a value works
        agentConfig.setServerHost("my.host.example.com");
        assertEquals("my.host.example.com", agentConfig.getServerHost());
    }

    @Test
    public void testSetServerPort() {
        final int DefaultServerPort = 1414;
        AgentConfig agentConfig = new AgentConfig();

        // Assert the default comes through
        assertEquals(DefaultServerPort, agentConfig.getServerPort());

        // Setting it zero returns default
        agentConfig.setServerPort(0);
        assertEquals(DefaultServerPort, agentConfig.getServerPort());

        // Setting it high returns default
        agentConfig.setServerPort(100000);
        assertEquals(DefaultServerPort, agentConfig.getServerPort());

        // Setting it to a value works
        agentConfig.setServerPort(22222);
        assertEquals(22222, agentConfig.getServerPort());
    }

    @Test
    public void testSetServerAuthUser() {
    }

    @Test
    public void testSetServerAuthPassword() {
    }

    @Test
    public void testSetServerChannelName() {
    }

    @Test
    public void testSetEventType() {
        final String DefaultEventType = "IBMMQSample";
        AgentConfig agentConfig = new AgentConfig();

        // Assert the default comes through
        assertEquals(DefaultEventType, agentConfig.getEventType());

        // Setting it Null returns default
        agentConfig.setEventType(null);
        assertEquals(DefaultEventType, agentConfig.getEventType());

        // Setting it Empty returns default
        agentConfig.setEventType("");
        assertEquals(DefaultEventType, agentConfig.getEventType());

        // Setting it to a value works
        agentConfig.setEventType("MyEventType");
        assertEquals("MyEventType", agentConfig.getEventType());

        assertEquals("MyEventTypeQueue", agentConfig.getEventType("Queue"));
        agentConfig.setVersion(1);
        assertEquals("MyEventType", agentConfig.getEventType("Queue"));
    }
}
