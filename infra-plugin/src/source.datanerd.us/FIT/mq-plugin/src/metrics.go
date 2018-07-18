package main

import (
	"context"
	"io"
	"log"
	"strconv"

	"github.com/newrelic/infra-integrations-sdk/integration"
	"source.datanerd.us/FIT/mq-plugin/src/rpc"

	"github.com/newrelic/infra-integrations-sdk/data/metric"
	"google.golang.org/grpc"
)

// PopulateInventory connects to the plugin service (RPC endpoint) and fetches all inventory items (streaming) as they are reported
func populateInventory(integration *integration.Integration, serviceHost string, servicePort int, serviceInstance string) error {
	opts := []grpc.DialOption{grpc.WithInsecure()}
	conn, err := grpc.Dial(serviceHost+":"+strconv.Itoa(servicePort), opts...)
	if err != nil {
		return err
	}
	defer close(conn)

	client := rpc.NewPluginServiceClient(conn)
	requestParameters := make(map[string]string)
	requestParameters["instance"] = serviceInstance
	request := &rpc.InventoryRequest{}
	request.Args = map[string]string{"instance": serviceInstance}
	stream, err := client.GetInventory(context.Background(), request)
	if err != nil {
		return err
	}
	for {
		res, err := stream.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		inventoryName := res.GetInventoryName()

		for field, value := range res.Inventory {
			err = integration.LocalEntity().SetInventoryItem(inventoryName, field, value)
			if err != nil {
				log.Fatal(err)
			}
		}
	}
	return nil
}

//PopulateMetrics connects to the plugin service (RPC endpoint) and fetches all metrics (streaming) as they are reported
func populateMetrics(integration *integration.Integration, serviceHost string, servicePort int, serviceInstance string) error {
	opts := []grpc.DialOption{grpc.WithInsecure()}
	conn, err := grpc.Dial(serviceHost+":"+strconv.Itoa(servicePort), opts...)
	if err != nil {
		return err
	}
	defer close(conn)

	client := rpc.NewPluginServiceClient(conn)
	requestParameters := make(map[string]string)
	requestParameters["instance"] = serviceInstance
	request := &rpc.MetricRequest{}
	request.Args = map[string]string{"instance": serviceInstance}
	stream, err := client.GetMetrics(context.Background(), request)
	if err != nil {
		return err
	}
	for {
		res, err := stream.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}

		responseName := res.GetMetricSetName()

		ms, err := integration.LocalEntity().NewMetricSet(responseName)
		if err != nil {
			return err
		}
		for _, metricData := range res.GetMetricSet() {
			metricType := metric.ATTRIBUTE
			switch metricData.GetSourceType() {
			case rpc.SourceType_GAUGE:
				metricType = metric.GAUGE

			case rpc.SourceType_RATE:
				metricType = metric.RATE

			case rpc.SourceType_DELTA:
				metricType = metric.DELTA

			case rpc.SourceType_ATTRIBUTE:
				metricType = metric.ATTRIBUTE
			}

			switch metricData.GetMetricValue().(type) {
			case *rpc.MetricData_StringValue:
				err = ms.SetMetric(metricData.GetMetricName(), metricData.GetStringValue(), metric.ATTRIBUTE)
				if err != nil {
					log.Fatal(err)
				}

			case *rpc.MetricData_DoubleValue:
				err = ms.SetMetric(metricData.GetMetricName(), metricData.GetDoubleValue(), metricType)
				if err != nil {
					log.Fatal(err)
				}

			case *rpc.MetricData_FloatValue:
				err = ms.SetMetric(metricData.GetMetricName(), metricData.GetFloatValue(), metricType)
				if err != nil {
					log.Fatal(err)
				}

			case *rpc.MetricData_IntValue:
				err = ms.SetMetric(metricData.GetMetricName(), metricData.GetIntValue(), metricType)
				if err != nil {
					log.Fatal(err)
				}

			case *rpc.MetricData_LongValue:
				err = ms.SetMetric(metricData.GetMetricName(), metricData.GetLongValue(), metricType)
				if err != nil {
					log.Fatal(err)
				}

			case *rpc.MetricData_BooleanValue:
				err = ms.SetMetric(metricData.GetMetricName(), metricData.GetBooleanValue(), metricType)
				if err != nil {
					log.Fatal(err)
				}

			default:
				err = ms.SetMetric(metricData.GetMetricName(), metricData.GetStringValue(), metric.ATTRIBUTE)
				if err != nil {
					log.Fatal(err)
				}
			}
		}
	}
	return nil
}

func close(c io.Closer) {
	err := c.Close()
	if err != nil {
		log.Fatal(err)
	}
}
