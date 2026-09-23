package com.impactflow.analysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "complexity_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String submissionId;

    private Integer totalLocDelta;

    private Integer cyclomaticComplexity;

    private Double fileChurnRate;
}
