package com.impactflow.analysis.controller;

import com.impactflow.analysis.domain.ChangeSubmission;
import com.impactflow.analysis.domain.ComplexityMetrics;
import com.impactflow.analysis.domain.DependencyNode;
import com.impactflow.analysis.dto.ChangeSubmittedEvent;
import com.impactflow.analysis.repository.ChangeSubmissionRepository;
import com.impactflow.analysis.service.ComplexityCalculator;
import com.impactflow.analysis.service.DependencyGraphManager;
import com.impactflow.analysis.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/changes")
public class ChangeAnalysisController {

    @Autowired
    private ChangeSubmissionRepository submissionRepository;

    @Autowired
    private DependencyGraphManager graphManager;

    @Autowired
    private ComplexityCalculator complexityCalculator;

    @Autowired
    private KafkaProducerService producerService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitChange(@RequestBody ChangeSubmission submission) {
        if (submission.getId() == null || submission.getId().isEmpty()) {
            submission.setId(UUID.randomUUID().toString());
        }
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus("PENDING");

        // Save submission in PENDING state
        ChangeSubmission savedSubmission = submissionRepository.save(submission);

        // Publish to Kafka for asynchronous enrichment and analysis
        ChangeSubmittedEvent event = ChangeSubmittedEvent.builder()
                .submission(savedSubmission)
                .build();
        producerService.sendChangeSubmitted(event);

        return ResponseEntity.accepted().body(savedSubmission);
    }

    @PostMapping("/dependencies")
    public ResponseEntity<?> registerDependency(@RequestParam("serviceName") String serviceName,
                                                 @RequestBody List<String> dependencies) {
        graphManager.registerDependency(serviceName, dependencies);
        return ResponseEntity.ok("Dependencies registered for " + serviceName);
    }

    @GetMapping("/dependencies/impacted")
    public ResponseEntity<?> getImpactedServices(@RequestParam("serviceName") String serviceName) {
        List<String> impacted = graphManager.calculateImpactedServices(serviceName);
        return ResponseEntity.ok(impacted);
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReport(@PathVariable("id") String id) {
        Optional<ChangeSubmission> subOpt = submissionRepository.findById(id);
        if (subOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChangeSubmission submission = subOpt.get();
        ComplexityMetrics metrics = complexityCalculator.getMetricsBySubmission(id);
        String serviceName = extractServiceFromFiles(submission.getChangedFiles());
        List<String> impactedServices = graphManager.calculateImpactedServices(serviceName);

        Map<String, Object> response = new HashMap<>();
        response.put("submission", submission);
        response.put("metrics", metrics);
        response.put("sourceService", serviceName);
        response.put("impactedServices", impactedServices);

        return ResponseEntity.ok(response);
    }

    private String extractServiceFromFiles(List<String> files) {
        if (files == null || files.isEmpty()) {
            return "unknown-service";
        }
        // Extract service name from file path if structure resembles: services/service-name/...
        for (String file : files) {
            String normalized = file.replace("\\", "/");
            if (normalized.startsWith("services/")) {
                String[] parts = normalized.split("/");
                if (parts.length > 2) {
                    return parts[1];
                }
            }
        }
        return "change-analysis-service"; // Fallback to current service
    }
}
