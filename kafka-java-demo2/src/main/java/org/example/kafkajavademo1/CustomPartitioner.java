package org.example.kafkajavademo1;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import java.util.Map;

public class CustomPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object keyObj, byte[] keyBytes,
                         Object value, byte[] valueBytes, Cluster cluster) {
        String key = (String) keyObj;
        int numPartitions = cluster.partitionCountForTopic(topic);

        if (key.startsWith("VIP")) {
            return 0; // VIP messages always go to partition 0
        } else {
            return Math.abs(key.hashCode()) % numPartitions;
        }
    }
    @Override public void close() {}
    @Override public void configure(Map<String, ?> configs) {}
}
