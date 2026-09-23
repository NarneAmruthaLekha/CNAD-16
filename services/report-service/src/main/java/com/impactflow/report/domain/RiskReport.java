package com.impactflow.report.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "risk_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskReport {

    @Id
    private String id; // UUID matching submissionId

    @Column(nullable = false)
    private String repoUrl;

    @Column(nullable = false)
    private String commitId;

    private String branch;

    private LocalDateTime submittedAt;

    private String status; // PENDING, COMPLETED, FAILED

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_changed_files", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "file_path")
    @Builder.Default
    private List<String> changedFiles = new ArrayList<>();

    // Metrics
    private Integer totalLocDelta;
    private Integer cyclomaticComplexity;
    private Double fileChurnRate;
    private String sourceService;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_impacted_services", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "service_name")
    @Builder.Default
    private List<String> impactedServices = new ArrayList<>();

    // Prediction
    private String riskLevel;
    private Double confidence;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_contributors", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "contributor")
    @Builder.Default
    private List<String> contributors = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_recommendations", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "recommendation")
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    private LocalDateTime generatedAt;
}
