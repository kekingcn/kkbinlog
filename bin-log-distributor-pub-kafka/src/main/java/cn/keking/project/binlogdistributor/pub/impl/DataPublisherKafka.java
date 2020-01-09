package cn.keking.project.binlogdistributor.pub.impl;

public interface DataPublisherKafka {

    void doPublish(String topic, Object data);

    boolean deleteTopic(String topicName);

}
