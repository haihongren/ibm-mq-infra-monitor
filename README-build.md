#Build

## Build MQ Monitoring Service(Java)

IBM has a restrictive license for its MQ Client libraries. So we cannot redistribute a copy of those jars. But the MQ Client jars are needed both for building and running the New Relic MQ Monitor. 
The following steps will make the MQ Client libraries available.

1. Copy jars from your local MQ installation into a folder named `mqlib`.
2. Update the startup script and edit the MQ_LIB environment variable to point to the folder containing the MQ client jars.

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

