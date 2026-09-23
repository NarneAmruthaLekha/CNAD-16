package com.impactflow.analysis.service;

import com.impactflow.analysis.domain.ChangeSubmission;
import com.impactflow.analysis.domain.ComplexityMetrics;
import com.impactflow.analysis.dto.ChangeEnrichedEvent;
import com.impactflow.analysis.dto.ChangeSubmittedEvent;
import com.impactflow.analysis.repository.ChangeSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KafkaMessagingTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ChangeSubmissionRepository submissionRepository;

    @Mock
    private ComplexityCalculator complexityCalculator;

    @Mock
    private DependencyGraphManager graphManager;

    @Mock
    private KafkaProducerService mockProducerService;

    @InjectMocks
    private KafkaProducerService producerService;

    @InjectMocks
    private KafkaConsumerService consumerService;

    @Test
    public void testProducerSendsMessage() {
        ChangeSubmission submission = ChangeSubmission.builder()
                .id("test-uuid")
                .repoUrl("https://github.com/test/repo")
                .commitId("abc1234")
                .branch("main")
                .submittedAt(LocalDateTime.now())
                .status("PENDING")
                .changedFiles(Collections.singletonList("services/auth-service/file.java"))
                .build();

        ChangeSubmittedEvent event = new ChangeSubmittedEvent(submission);

        producerService.sendChangeSubmitted(event);

        verify(kafkaTemplate).send(eq("change.submitted"), eq("test-uuid"), eq(event));
    }

    @Test
    public void testConsumerProcessesAndPublishes() {
        ChangeSubmission submission = ChangeSubmission.builder()
                .id("test-uuid")
                .repoUrl("https://github.com/test/repo")
                .commitId("abc1234")
                .branch("main")
                .submittedAt(LocalDateTime.now())
                .status("PENDING")
                .changedFiles(Collections.singletonList("services/auth-service/file.java"))
                .build();

        ChangeSubmittedEvent submittedEvent = new ChangeSubmittedEvent(submission);

        ComplexityMetrics metrics = ComplexityMetrics.builder()
                .submissionId("test-uuid")
                .totalLocDelta(100)
                .cyclomaticComplexity(5)
                .fileChurnRate(0.2)
                .build();

        when(submissionRepository.findById("test-uuid")).thenReturn(Optional.of(submission));
        when(complexityCalculator.calculateMetrics(eq("test-uuid"), any())).thenReturn(metrics);
        when(graphManager.calculateImpactedServices("auth-service")).thenReturn(Collections.singletonList("api-gateway"));

        consumerService.consumeChangeSubmitted(submittedEvent);

        verify(submissionRepository).save(any(ChangeSubmission.class));
        
        ArgumentCaptor<ChangeEnrichedEvent> captor = ArgumentCaptor.forClass(ChangeEnrichedEvent.class);
        verify(mockProducerService).sendChangeEnriched(captor.capture());
        
        ChangeEnrichedEvent enrichedEvent = captor.getValue();
        assertNotNull(enrichedEvent);
        assertEquals("test-uuid", enrichedEvent.getSubmissionId());
        assertEquals(100, enrichedEvent.getTotalLocDelta());
        assertEquals(5, enrichedEvent.getCyclomaticComplexity());
        assertEquals(0.2, enrichedEvent.getFileChurnRate());
        assertEquals("auth-service", enrichedEvent.getSourceService());
        assertEquals(1, enrichedEvent.getImpactedServices().size());
        assertEquals("api-gateway", enrichedEvent.getImpactedServices().get(0));
    }
}
