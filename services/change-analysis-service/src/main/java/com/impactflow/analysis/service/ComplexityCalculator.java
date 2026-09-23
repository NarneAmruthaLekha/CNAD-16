package com.impactflow.analysis.service;

import com.impactflow.analysis.domain.ComplexityMetrics;
import com.impactflow.analysis.repository.ComplexityMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplexityCalculator {

    @Autowired
    private ComplexityMetricsRepository metricsRepository;

    public ComplexityMetrics calculateMetrics(String submissionId, List<String> changedFiles) {
        int locDelta = 0;
        int complexity = 1; // Base complexity
        
        if (changedFiles != null && !changedFiles.isEmpty()) {
            for (String file : changedFiles) {
                String lowercaseFile = file.toLowerCase();
                if (lowercaseFile.endsWith(".java")) {
                    locDelta += 75; // average Java class changes
                    complexity += 5; // Java methods complexity weight
                } else if (lowercaseFile.endsWith(".py") || lowercaseFile.endsWith(".js") || lowercaseFile.endsWith(".ts")) {
                    locDelta += 40;
                    complexity += 3;
                } else if (lowercaseFile.endsWith(".xml") || lowercaseFile.endsWith(".yml") || lowercaseFile.endsWith(".yaml")) {
                    locDelta += 15;
                    complexity += 1;
                } else {
                    locDelta += 10;
                }
            }
        }

        // Churn rate = (changed files / total system size estimate)
        double totalSystemFilesEstimate = 50.0;
        double churn = (changedFiles != null ? changedFiles.size() : 0) / totalSystemFilesEstimate;

        ComplexityMetrics metrics = ComplexityMetrics.builder()
                .submissionId(submissionId)
                .totalLocDelta(locDelta)
                .cyclomaticComplexity(complexity)
                .fileChurnRate(churn)
                .build();

        return metricsRepository.save(metrics);
    }

    public ComplexityMetrics getMetricsBySubmission(String submissionId) {
        return metricsRepository.findBySubmissionId(submissionId).orElse(null);
    }
}
