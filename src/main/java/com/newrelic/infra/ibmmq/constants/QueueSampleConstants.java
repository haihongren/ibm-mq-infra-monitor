package com.newrelic.infra.ibmmq.constants;

public interface QueueSampleConstants {

    String OLDEST_MSG_AGE= "oldestMsgAge";
    String UNCOMITTED_MSGS="uncommittedMsgs";
    String LAST_GET_DATE_TIME= "lastGet";
    String LAST_PUT_DATE_TIME="lastPut";

    String Q_DEPTH="qDepth";
    String Q_MAX_DEPTH="qDepthMax";
    String OPEN_INPUT_COUNT="openInputCount";
    String OPEN_OUTPUT_COUNT="openOutputCount";

    String Q_DEPTH_PERCENT="qDepthPercent";

    String HIGH_Q_DEPTH="highQDepth";
    String MSG_DEQ_COUNT="msgDeqCount";
    String MSG_ENQ_COUNT="msgEnqCount";
    String TIME_SINCE_RESET="timeSinceReset";

    String MQ_QUEUE_SAMPLE="MQQueueSample";

}

