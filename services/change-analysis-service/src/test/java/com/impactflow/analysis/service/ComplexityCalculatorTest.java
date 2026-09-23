package com.impactflow.analysis.service;

import com.impactflow.analysis.domain.ComplexityMetrics;
import com.impactflow.analysis.repository.ComplexityMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ComplexityCalculatorTest {

    @InjectMocks
    private ComplexityCalculator complexityCalculator;

    @Mock
    private ComplexityMetricsRepository metricsRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Just return the input entity when save is called
        when(metricsRepository.save(any(ComplexityMetrics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testCalculateMetricsJavaFile() {
        List<String> files = Collections.singletonList("services/auth-service/src/main/java/User.java");
        ComplexityMetrics metrics = complexityCalculator.calculateMetrics("sub-123", files);

        assertNotNull(metrics);
        assertEquals("sub-123", metrics.getSubmissionId());
        assertEquals(75, metrics.getTotalLocDelta()); // 75 per java file
        assertEquals(6, metrics.getCyclomaticComplexity()); // 1 base + 5 java weight
        assertEquals(0.02, metrics.getFileChurnRate()); // 1 / 50 total estimate
    }

    @Test
    public void testCalculateMetricsMultipleFiles() {
        List<String> files = Arrays.asList(
                "services/auth-service/src/main/java/User.java",
                "services/auth-service/src/main/resources/application.yml",
                "docs/README.md"
        );
        ComplexityMetrics metrics = complexityCalculator.calculateMetrics("sub-456", files);

        assertNotNull(metrics);
        assertEquals(75 + 15 + 10, metrics.getTotalLocDelta()); // 75 java + 15 yaml + 10 other
        assertEquals(1 + 5 + 1, metrics.getCyclomaticComplexity()); // 1 base + 5 java + 1 yaml
        assertEquals(0.06, metrics.getFileChurnRate()); // 3 / 50 total estimate
    }
}
