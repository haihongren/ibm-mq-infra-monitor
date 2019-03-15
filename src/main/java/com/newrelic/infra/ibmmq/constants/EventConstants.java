package com.newrelic.infra.ibmmq.constants;

public interface EventConstants {
    // standard event attributes
    String Q_MANAGER_NAME="qManagerName";
    String Q_MANAGER_HOST="qManagerHost";

    String Q_NAME="qName";
    String TOPIC_NAME="topicName";
    String OBJECT_ATTRIBUTE="object";

    String PROVIDER="provider";
    String IBM_PROVIDER="IBM";

    String NAME="name";
    String ERROR="error";
    String STATUS="status";

    // OBJECT_ATTRIBUTE TYPEs
    String OBJ_ATTR_TYPE_QUEUE ="queue";
	Object OBJ_ATTR_TYPE_TOPIC = "topic";
    String OBJ_ATTR_TYPE_Q_MGR ="QueueManager";
    String OBJ_ATTR_TYPE_Q_LISTENER="Listener";
    
	String DURABLE = "durable";
	String SUB_ID = "subId";
	String PUB_COUNT = "pubCount";
	String SUB_COUNT = "subCount";
	String SUB_USER_ID = "subUserId";

}
