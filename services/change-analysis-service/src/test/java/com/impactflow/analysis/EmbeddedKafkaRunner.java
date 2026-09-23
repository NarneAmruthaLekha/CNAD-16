package com.impactflow.analysis;

import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

public class EmbeddedKafkaRunner {
    public static void main(String[] args) {
        try {
            System.out.println("Starting Embedded ZooKeeper + Kafka Broker on port 9092...");
            EmbeddedKafkaZKBroker broker = new EmbeddedKafkaZKBroker(1, true, "change.submitted", "change.enriched", "risk.scored");
            broker.kafkaPorts(9092);
            broker.afterPropertiesSet();
            System.out.println("EMBEDDED_KAFKA_READY on " + broker.getBrokersAsString());
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
