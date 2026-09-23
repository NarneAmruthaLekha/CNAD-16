package com.impactflow.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impactflow.report.domain.RiskReport;
import com.impactflow.report.dto.ChangeEnrichedEvent;
import com.impactflow.report.dto.ChangeSubmittedEvent;
import com.impactflow.report.dto.RiskScoredEvent;
import com.impactflow.report.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(
            topics = "change.submitted",
            groupId = "report-group-submitted",
            properties = {"value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"}
    )
    public void consumeChangeSubmitted(String payload) {
        logger.info("Received change.submitted raw event: {}", payload);
        try {
            ChangeSubmittedEvent event = objectMapper.readValue(payload, ChangeSubmittedEvent.class);
            if (event.getSubmission() == null) {
                logger.warn("Received change.submitted event with empty submission");
                return;
            }
            String id = event.getSubmission().getId();
            
            // Synchronize on the interned ID string to prevent dirty read/lost update race conditions
            synchronized (id.intern()) {
                RiskReport report = reportRepository.findById(id).orElse(new RiskReport());
                report.setId(id);
                report.setRepoUrl(event.getSubmission().getRepoUrl());
                report.setCommitId(event.getSubmission().getCommitId());
                report.setBranch(event.getSubmission().getBranch());
                report.setSubmittedAt(event.getSubmission().getSubmittedAt() != null ? 
                        event.getSubmission().getSubmittedAt() : LocalDateTime.now());
                report.setChangedFiles(event.getSubmission().getChangedFiles());
                
                if (report.getStatus() == null) {
                    report.setStatus("PENDING");
                }
                
                reportRepository.saveAndFlush(report);
                logger.info("Saved skeletal report for submission ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error processing change.submitted event", e);
        }
    }

    @KafkaListener(
            topics = "change.enriched",
            groupId = "report-group-enriched",
            properties = {"value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"}
    )
    public void consumeChangeEnriched(String payload) {
        logger.info("Received change.enriched raw event: {}", payload);
        try {
            ChangeEnrichedEvent event = objectMapper.readValue(payload, ChangeEnrichedEvent.class);
            String id = event.getSubmissionId();
            
            // Synchronize on the interned ID string to prevent dirty read/lost update race conditions
            synchronized (id.intern()) {
                RiskReport report = reportRepository.findById(id).orElse(new RiskReport());
                report.setId(id);
                report.setTotalLocDelta(event.getTotalLocDelta());
                report.setCyclomaticComplexity(event.getCyclomaticComplexity());
                report.setFileChurnRate(event.getFileChurnRate());
                report.setSourceService(event.getSourceService());
                report.setImpactedServices(event.getImpactedServices());
                
                if (report.getStatus() == null) {
                    report.setStatus("PENDING");
                }
                
                checkAndCompleteReport(report);
                
                reportRepository.saveAndFlush(report);
                logger.info("Merged enriched metrics for submission ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error processing change.enriched event", e);
        }
    }

    @KafkaListener(
            topics = "risk.scored",
            groupId = "report-group-scored",
            properties = {"value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"}
    )
    public void consumeRiskScored(String payload) {
        logger.info("Received risk.scored raw event: {}", payload);
        try {
            RiskScoredEvent event = objectMapper.readValue(payload, RiskScoredEvent.class);
            String id = event.getSubmissionId();
            
            // Synchronize on the interned ID string to prevent dirty read/lost update race conditions
            synchronized (id.intern()) {
                RiskReport report = reportRepository.findById(id).orElse(new RiskReport());
                report.setId(id);
                report.setRiskLevel(event.getRiskLevel());
                report.setConfidence(event.getConfidence());
                report.setContributors(event.getContributors());
                
                if (report.getStatus() == null) {
                    report.setStatus("PENDING");
                }
                
                checkAndCompleteReport(report);
                
                reportRepository.saveAndFlush(report);
                logger.info("Merged risk prediction for submission ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error processing risk.scored event", e);
        }
    }

    private void checkAndCompleteReport(RiskReport report) {
        if (report.getRiskLevel() != null && report.getTotalLocDelta() != null) {
            report.setStatus("COMPLETED");
            report.setGeneratedAt(LocalDateTime.now());
            
            List<String> recs = generateRecommendations(
                    report.getRiskLevel(),
                    report.getImpactedServices(),
                    report.getCyclomaticComplexity() != null ? report.getCyclomaticComplexity() : 0
            );
            report.setRecommendations(recs);
        }
    }

    private List<String> generateRecommendations(String riskLevel, List<String> impactedServices, int complexity) {
        List<String> recommendations = new ArrayList<>();
        if ("High".equalsIgnoreCase(riskLevel)) {
            recommendations.add("CRITICAL: Code changes carry high risk. Require Senior Architect / Tech Lead approval before merging.");
            recommendations.add("TESTING: Execute complete integration testing and run performance benchmarks.");
            if (impactedServices != null && !impactedServices.isEmpty()) {
                recommendations.add("IMPACT: Downstream services " + impactedServices + " could be broken. Coordinate manual smoke tests with owners of these services.");
            }
            if (complexity > 20) {
                recommendations.add("REFACTOR: Cyclomatic complexity is extremely high (" + complexity + "). Consider breaking down large methods.");
            }
        } else if ("Medium".equalsIgnoreCase(riskLevel)) {
            recommendations.add("WARNING: Moderate risk change. Peer review by at least one experienced developer required.");
            recommendations.add("TESTING: Trigger fast regression tests and verification in staging environment.");
            if (impactedServices != null && !impactedServices.isEmpty()) {
                recommendations.add("IMPACT: Monitor downstream services " + impactedServices + " for regression.");
            }
        } else {
            recommendations.add("INFO: Low risk change. Standard automated deployment permitted upon passing unit tests.");
        }
        return recommendations;
    }
}
