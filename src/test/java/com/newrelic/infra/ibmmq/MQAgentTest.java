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
        MQAgent testAgent = new MQAgent();

        // Assert the default comes through
        assertEquals(DefaultServerHost, testAgent.getServerHost());

        // Setting it Null returns default
        testAgent.setServerHost(null);
        assertEquals(DefaultServerHost, testAgent.getServerHost());

        // Setting it Empty returns default
        testAgent.setServerHost("");
        assertEquals(DefaultServerHost, testAgent.getServerHost());

        // Setting it to a value works
        testAgent.setServerHost("my.host.example.com");
        assertEquals("my.host.example.com", testAgent.getServerHost());
    }

    @Test
    public void testSetServerPort() {
        final int DefaultServerPort = 1414;
        MQAgent testAgent = new MQAgent();

        // Assert the default comes through
        assertEquals(DefaultServerPort, testAgent.getServerPort());

        // Setting it zero returns default
        testAgent.setServerPort(0);
        assertEquals(DefaultServerPort, testAgent.getServerPort());

        // Setting it high returns default
        testAgent.setServerPort(100000);
        assertEquals(DefaultServerPort, testAgent.getServerPort());

        // Setting it to a value works
        testAgent.setServerPort(22222);
        assertEquals(22222, testAgent.getServerPort());
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
        MQAgent testAgent = new MQAgent();

        // Assert the default comes through
        assertEquals(DefaultEventType, testAgent.getEventType());

        // Setting it Null returns default
        testAgent.setEventType(null);
        assertEquals(DefaultEventType, testAgent.getEventType());

        // Setting it Empty returns default
        testAgent.setEventType("");
        assertEquals(DefaultEventType, testAgent.getEventType());

        // Setting it to a value works
        testAgent.setEventType("MyEventType");
        assertEquals("MyEventType", testAgent.getEventType());
    }
}
