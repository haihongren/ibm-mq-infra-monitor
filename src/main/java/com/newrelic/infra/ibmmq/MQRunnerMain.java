package com.newrelic.infra.ibmmq;

import com.newrelic.infra.publish.RunnerFactory;
import com.newrelic.infra.publish.api.Runner;

public class MQRunnerMain {
	public static void main(String[] args) {
        try {
        		Runner runner = RunnerFactory.getRunner();
            runner.add(new MQMonitorAgentFactory());
            runner.setupAndRun(); // Never returns
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(-1);
        }
    }
}
