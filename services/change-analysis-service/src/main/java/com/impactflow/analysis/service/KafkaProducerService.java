package com.impactflow.analysis.service;

import com.impactflow.analysis.dto.ChangeEnrichedEvent;
import com.impactflow.analysis.dto.ChangeSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendChangeSubmitted(ChangeSubmittedEvent event) {
        String topic = "change.submitted";
        String key = event.getSubmission().getId();
        logger.info("Publishing ChangeSubmittedEvent to topic {} with key {}: {}", topic, key, event);
        kafkaTemplate.send(topic, key, event);
    }

    public void sendChangeEnriched(ChangeEnrichedEvent event) {
        String topic = "change.enriched";
        String key = event.getSubmissionId();
        logger.info("Publishing ChangeEnrichedEvent to topic {} with key {}: {}", topic, key, event);
        kafkaTemplate.send(topic, key, event);
    }
}
