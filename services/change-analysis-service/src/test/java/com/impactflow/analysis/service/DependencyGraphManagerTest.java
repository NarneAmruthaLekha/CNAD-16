package com.impactflow.analysis.service;

import com.impactflow.analysis.domain.DependencyNode;
import com.impactflow.analysis.repository.DependencyNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class DependencyGraphManagerTest {

    @InjectMocks
    private DependencyGraphManager graphManager;

    @Mock
    private DependencyNodeRepository nodeRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCalculateImpactedServicesBFS() {
        // Construct a mock graph:
        // auth-service has dependents [change-analysis-service, api-gateway]
        // change-analysis-service has dependents [report-service]
        // Let's mock nodes:
        
        DependencyNode authNode = DependencyNode.builder()
                .serviceName("auth-service")
                .dependents(Arrays.asList("change-analysis-service", "api-gateway"))
                .build();
        
        DependencyNode analysisNode = DependencyNode.builder()
                .serviceName("change-analysis-service")
                .dependents(Collections.singletonList("report-service"))
                .build();
        
        DependencyNode gatewayNode = DependencyNode.builder()
                .serviceName("api-gateway")
                .dependents(new ArrayList<>())
                .build();
        
        DependencyNode reportNode = DependencyNode.builder()
                .serviceName("report-service")
                .dependents(new ArrayList<>())
                .build();

        when(nodeRepository.findByServiceName("auth-service")).thenReturn(Optional.of(authNode));
        when(nodeRepository.findByServiceName("change-analysis-service")).thenReturn(Optional.of(analysisNode));
        when(nodeRepository.findByServiceName("api-gateway")).thenReturn(Optional.of(gatewayNode));
        when(nodeRepository.findByServiceName("report-service")).thenReturn(Optional.of(reportNode));

        List<String> impacted = graphManager.calculateImpactedServices("auth-service");

        // The expected traversal sequence via BFS:
        // Level 1: change-analysis-service, api-gateway
        // Level 2: report-service
        assertEquals(3, impacted.size());
        assertTrue(impacted.contains("change-analysis-service"));
        assertTrue(impacted.contains("api-gateway"));
        assertTrue(impacted.contains("report-service"));
    }
}
