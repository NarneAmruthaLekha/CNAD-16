
package com.impactflow.analysis.controller;

import com.impactflow.analysis.domain.ChangeSubmission;
import com.impactflow.analysis.dto.ChangeSubmissionRequest;
import com.impactflow.analysis.dto.ChangeSubmittedEvent;
import com.impactflow.analysis.dto.RiskPredictionResponse;
import com.impactflow.analysis.repository.ChangeSubmissionRepository;
import com.impactflow.analysis.service.GitHubService;
import com.impactflow.analysis.service.KafkaProducerService;
import com.impactflow.analysis.service.RiskPredictionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/changes")
public class ChangeAnalysisController {

    private final ChangeSubmissionRepository repository;
    private final KafkaProducerService kafkaProducerService;
    private final RiskPredictionService riskPredictionService;
    private final GitHubService gitHubService;

    public ChangeAnalysisController(
            ChangeSubmissionRepository repository,
            KafkaProducerService kafkaProducerService,
            RiskPredictionService riskPredictionService,
            GitHubService gitHubService) {
        this.repository = repository;
        this.kafkaProducerService = kafkaProducerService;
        this.riskPredictionService = riskPredictionService;
        this.gitHubService = gitHubService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(
            @Valid @RequestBody ChangeSubmissionRequest request) {

        try {
            Map<String, Object> data = gitHubService.analyzeCommit(
                    request.getRepoUrl(),
                    request.getCommitId()
            );

            @SuppressWarnings("unchecked")
            List<String> files =
                    (List<String>) data.get("changedFiles");

            int added = (Integer) data.get("linesAdded");
            int deleted = (Integer) data.get("linesDeleted");
            int complexity = (Integer) data.get("complexity");

            RiskPredictionResponse prediction = riskPredictionService.predict(
                    files.size(),
                    added,
                    deleted,
                    complexity
            );

            ChangeSubmission submission = new ChangeSubmission();
            submission.setId(UUID.randomUUID().toString());
            submission.setRepoUrl(request.getRepoUrl());
            submission.setCommitId((String) data.get("commitId"));
            submission.setBranch(request.getBranch());
            submission.setProposedChange(
                    (String) data.get("commitMessage"));
            submission.setChangedFiles(files);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setStatus("PENDING");

            submission.setRisk(prediction.getRisk());
            submission.setRiskScore(prediction.getRisk_score());
            submission.setRiskMessage(prediction.getMessage());

            submission.setLinesAdded(added);
            submission.setLinesDeleted(deleted);
            submission.setComplexity(complexity);

            ChangeSubmission saved = repository.save(submission);

            kafkaProducerService.sendChangeSubmitted(
                    new ChangeSubmittedEvent(saved)
            );

            return ResponseEntity.accepted().body(Map.of(
                    "submission", saved,
                    "riskPrediction", prediction,
                    "githubAnalysis", data
            ));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "GitHub analysis was interrupted")
            );

        } catch (IOException | IllegalArgumentException
                 | IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage() == null
                            ? "GitHub analysis failed"
                            : e.getMessage())
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChangeSubmission> getSubmission(
            @PathVariable String id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
