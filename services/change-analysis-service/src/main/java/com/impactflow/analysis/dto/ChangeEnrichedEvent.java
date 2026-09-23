package com.impactflow.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEnrichedEvent {
    private String submissionId;
    private int totalLocDelta;
    private int cyclomaticComplexity;
    private double fileChurnRate;
    private String sourceService;
    private List<String> impactedServices;
}
