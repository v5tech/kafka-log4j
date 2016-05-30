package net.aimeizi.kafka;

import com.google.common.collect.ImmutableMap;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyConsumer {
    private static final String ZOOKEEPER = "localhost:2181";
    //groupName可以随意给，对于kafka里的每条消息，每个group都会完整的处理一遍
    private static final String GROUP_NAME = "test_group";
    private static final String TOPIC_NAME = "kafka";
    private static final int CONSUMER_NUM = 4;
    private static final int PARTITION_NUM = 4;

    public static void main(String[] args) {
        // specify some consumer properties
        Properties props = new Properties();
        props.put("zookeeper.connect", ZOOKEEPER);
        props.put("zookeeper.connectiontimeout.ms", "1000000");
        props.put("group.id", GROUP_NAME);

        // Create the connection to the cluster
        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        ConsumerConnector consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);

        // create 4 partitions of the stream for topic “test”, to allow 4
        // threads to consume
        Map<String, List<KafkaStream<byte[], byte[]>>> topicMessageStreams = consumerConnector
                .createMessageStreams(ImmutableMap.of(TOPIC_NAME, PARTITION_NUM));
        List<KafkaStream<byte[], byte[]>> streams = topicMessageStreams.get(TOPIC_NAME);

        // create list of 4 threads to consume from each of the partitions
        ExecutorService executor = Executors.newFixedThreadPool(CONSUMER_NUM);

        // consume the messages in the threads
        for (final KafkaStream<byte[], byte[]> stream : streams) {
            executor.submit(new Runnable() {
                public void run() {
                    for (MessageAndMetadata<byte[], byte[]> msgAndMetadata : stream) {
                        // process message (msgAndMetadata.message())
                        System.out.println(new String(msgAndMetadata.message()));
                    }
                }
            });
        }
    }
}
