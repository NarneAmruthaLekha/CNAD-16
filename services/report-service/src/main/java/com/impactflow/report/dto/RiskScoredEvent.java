package com.impactflow.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoredEvent {
    private String submissionId;
    private String riskLevel;
    private double confidence;
    private List<String> contributors;
    private String predictedAt;
}
