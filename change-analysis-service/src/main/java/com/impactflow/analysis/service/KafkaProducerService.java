package com.impactflow.analysis.service;

import com.impactflow.analysis.dto.ChangeSubmittedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String,Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String,Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendChangeSubmitted(ChangeSubmittedEvent event) {
        kafkaTemplate.send(
                "change.submitted",
                event.getSubmission().getId(),
                event
        );
    }
}