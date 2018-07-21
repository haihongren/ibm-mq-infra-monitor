#Build

## Build MQ Monitoring Service(Java)

```bash
   ./gradlew clean build
```

Build artifacts will be in build/distribution.

## Build New Relic Infrastructure Agent Integration Plugin 

```bash
   cd nr-infra-integration
   source envrc
   ./installTools.sh

   cd src/github.com/newrelic-experts/mq-plugin
   make
```

