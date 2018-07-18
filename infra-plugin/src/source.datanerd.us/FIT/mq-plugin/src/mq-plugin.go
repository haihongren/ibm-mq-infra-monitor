package main

import (
	"log"
	"strings"

	sdkArgs "github.com/newrelic/infra-integrations-sdk/args"
	"github.com/newrelic/infra-integrations-sdk/integration"
)

type argumentList struct {
	sdkArgs.DefaultArgumentList
	JMXMonitoringServiceHost string `default:"localhost" help:"Hostname or IP where JMX Java Monitoring Service is running."`
	JMXMonitoringServicePort int    `default:"9001" help:"Port on which the JMX Java Monitoring Service is listening."`
	JMXConnectionName        string `default:"all" help:"JMX Hostname"`
}

const (
	integrationName    = "com.newrelic.infra-jmx-plugin"
	integrationVersion = "0.1.0"
)

var (
	args argumentList
)

func main() {
	// Create Integration
	i, err := integration.New(integrationName, integrationVersion, integration.Args(&args))
	fatalIfErr(err)

	// Add Event
	if args.All() || args.Events {
		//i.LocalEntity().AddEvent(event.New("restart", "status"))
	}

	// Add Inventory item
	if args.All() || args.Inventory {
		err := populateInventory(i, args.JMXMonitoringServiceHost, args.JMXMonitoringServicePort, strings.TrimSpace(args.JMXConnectionName))
		fatalIfErr(err)
	}

	// Add Metric
	if args.All() || args.Metrics {
		err := populateMetrics(i, args.JMXMonitoringServiceHost, args.JMXMonitoringServicePort, strings.TrimSpace(args.JMXConnectionName))
		fatalIfErr(err)
	}

	fatalIfErr(i.Publish())
}

func fatalIfErr(err error) {
	if err != nil {
		log.Fatal(err)
	}
}
