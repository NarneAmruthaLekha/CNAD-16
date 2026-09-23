package com.impactflow.analysis.repository;

import com.impactflow.analysis.domain.ComplexityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplexityMetricsRepository extends JpaRepository<ComplexityMetrics, Long> {
    Optional<ComplexityMetrics> findBySubmissionId(String submissionId);
}
