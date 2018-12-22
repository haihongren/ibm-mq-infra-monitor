#Build

## Build MQ Monitoring Service(Java)

```bash
   ./gradlew clean build
```

Build artifacts will be in build/distribution.

## Build New Relic Infrastructure Agent Integration Plugin 

The following is not needed. It is only a legacy artifact to allow routing the events though the infrastructure agent rather than HTTP POST using Event API.

```bash
   cd nr-infra-integration
   source envrc
   ./installTools.sh

   cd src/github.com/newrelic-experts/mq-plugin
   make
```

