package com.newrelic.infra.ibmmq;

import com.newrelic.infra.publish.RunnerFactory;
import com.newrelic.infra.publish.api.Runner;

public class Main {
	public static void main(String[] args) {
        try {
        		Runner runner = RunnerFactory.getRunner();
            runner.add(new MQAgentFactory());
            runner.setupAndRun(); // Blocks until CTRL-C
        } catch (Exception e) {
        	e.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
