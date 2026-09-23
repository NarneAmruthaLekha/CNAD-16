package com.impactflow.analysis.service;

import com.impactflow.analysis.domain.ChangeSubmission;
import com.impactflow.analysis.domain.ComplexityMetrics;
import com.impactflow.analysis.dto.ChangeEnrichedEvent;
import com.impactflow.analysis.dto.ChangeSubmittedEvent;
import com.impactflow.analysis.repository.ChangeSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private ChangeSubmissionRepository submissionRepository;

    @Autowired
    private ComplexityCalculator complexityCalculator;

    @Autowired
    private DependencyGraphManager graphManager;

    @Autowired
    private KafkaProducerService producerService;

    @KafkaListener(topics = "change.submitted", groupId = "impactflow-group")
    public void consumeChangeSubmitted(ChangeSubmittedEvent event) {
        logger.info("Received ChangeSubmittedEvent: {}", event);
        ChangeSubmission submission = event.getSubmission();
        if (submission == null) {
            logger.warn("Received empty submission in event");
            return;
        }

        try {
            // Retrieve submission from database (to handle persistence state correctly)
            Optional<ChangeSubmission> subOpt = submissionRepository.findById(submission.getId());
            if (subOpt.isEmpty()) {
                logger.warn("Submission with ID {} not found in database", submission.getId());
                return;
            }
            ChangeSubmission dbSubmission = subOpt.get();

            // Calculate Complexity metrics
            ComplexityMetrics metrics = complexityCalculator.calculateMetrics(
                    dbSubmission.getId(),
                    dbSubmission.getChangedFiles()
            );

            // Estimate impacted services
            String serviceName = extractServiceFromFiles(dbSubmission.getChangedFiles());
            List<String> impactedServices = graphManager.calculateImpactedServices(serviceName);

            // Update status
            dbSubmission.setStatus("ANALYZED");
            submissionRepository.save(dbSubmission);

            // Build and publish enrichment event
            ChangeEnrichedEvent enrichedEvent = ChangeEnrichedEvent.builder()
                    .submissionId(dbSubmission.getId())
                    .totalLocDelta(metrics.getTotalLocDelta())
                    .cyclomaticComplexity(metrics.getCyclomaticComplexity())
                    .fileChurnRate(metrics.getFileChurnRate())
                    .sourceService(serviceName)
                    .impactedServices(impactedServices)
                    .build();

            producerService.sendChangeEnriched(enrichedEvent);
            logger.info("Successfully processed and enriched submission ID: {}", dbSubmission.getId());

        } catch (Exception e) {
            logger.error("Error processing ChangeSubmittedEvent for submission ID: " + submission.getId(), e);
            Optional<ChangeSubmission> subOpt = submissionRepository.findById(submission.getId());
            subOpt.ifPresent(dbSub -> {
                dbSub.setStatus("FAILED");
                submissionRepository.save(dbSub);
            });
        }
    }

    private String extractServiceFromFiles(List<String> files) {
        if (files == null || files.isEmpty()) {
            return "unknown-service";
        }
        for (String file : files) {
            String normalized = file.replace("\\", "/");
            if (normalized.startsWith("services/")) {
                String[] parts = normalized.split("/");
                if (parts.length > 2) {
                    return parts[1];
                }
            }
        }
        return "change-analysis-service";
    }
}
