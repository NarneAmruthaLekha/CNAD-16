package com.impactflow.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.impactflow.report.domain.RiskReport;
import com.impactflow.report.dto.ChangeEnrichedEvent;
import com.impactflow.report.dto.ChangeSubmittedEvent;
import com.impactflow.report.dto.RiskScoredEvent;
import com.impactflow.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaConsumerServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Inject objectMapper into the service (since Mockito doesn't inject it automatically if it's not a mock)
        org.springframework.test.util.ReflectionTestUtils.setField(kafkaConsumerService, "objectMapper", objectMapper);
    }

    @Test
    public void testConsumeChangeSubmitted_NewReport() throws Exception {
        String submissionId = "sub-123";
        ChangeSubmittedEvent.ChangeSubmissionDto subDto = ChangeSubmittedEvent.ChangeSubmissionDto.builder()
                .id(submissionId)
                .repoUrl("https://github.com/org/repo")
                .commitId("c123")
                .branch("main")
                .changedFiles(List.of("file1.java"))
                .build();
        ChangeSubmittedEvent event = new ChangeSubmittedEvent(subDto);
        String payload = objectMapper.writeValueAsString(event);

        when(reportRepository.findById(submissionId)).thenReturn(Optional.empty());

        kafkaConsumerService.consumeChangeSubmitted(payload);

        ArgumentCaptor<RiskReport> reportCaptor = ArgumentCaptor.forClass(RiskReport.class);
        verify(reportRepository, times(1)).save(reportCaptor.capture());
        
        RiskReport savedReport = reportCaptor.getValue();
        assertEquals(submissionId, savedReport.getId());
        assertEquals("https://github.com/org/repo", savedReport.getRepoUrl());
        assertEquals("c123", savedReport.getCommitId());
        assertEquals("main", savedReport.getBranch());
        assertEquals(List.of("file1.java"), savedReport.getChangedFiles());
        assertEquals("PENDING", savedReport.getStatus());
    }

    @Test
    public void testConsumeChangeEnriched_MergesMetrics() throws Exception {
        String submissionId = "sub-123";
        ChangeEnrichedEvent event = ChangeEnrichedEvent.builder()
                .submissionId(submissionId)
                .totalLocDelta(250)
                .cyclomaticComplexity(12)
                .fileChurnRate(0.35)
                .sourceService("payment-service")
                .impactedServices(List.of("auth-service"))
                .build();
        String payload = objectMapper.writeValueAsString(event);

        RiskReport existingReport = RiskReport.builder()
                .id(submissionId)
                .repoUrl("https://github.com/org/repo")
                .commitId("c123")
                .status("PENDING")
                .build();

        when(reportRepository.findById(submissionId)).thenReturn(Optional.of(existingReport));

        kafkaConsumerService.consumeChangeEnriched(payload);

        ArgumentCaptor<RiskReport> reportCaptor = ArgumentCaptor.forClass(RiskReport.class);
        verify(reportRepository, times(1)).save(reportCaptor.capture());

        RiskReport savedReport = reportCaptor.getValue();
        assertEquals(submissionId, savedReport.getId());
        assertEquals(250, savedReport.getTotalLocDelta());
        assertEquals(12, savedReport.getCyclomaticComplexity());
        assertEquals(0.35, savedReport.getFileChurnRate());
        assertEquals("payment-service", savedReport.getSourceService());
        assertEquals(List.of("auth-service"), savedReport.getImpactedServices());
        assertEquals("PENDING", savedReport.getStatus()); // remains PENDING as risk score is missing
    }

    @Test
    public void testConsumeRiskScored_CompletesReport() throws Exception {
        String submissionId = "sub-123";
        RiskScoredEvent event = RiskScoredEvent.builder()
                .submissionId(submissionId)
                .riskLevel("High")
                .confidence(0.92)
                .contributors(List.of("High LOC"))
                .build();
        String payload = objectMapper.writeValueAsString(event);

        // Pre-existing report that already has metrics populated
        RiskReport existingReport = RiskReport.builder()
                .id(submissionId)
                .repoUrl("https://github.com/org/repo")
                .commitId("c123")
                .totalLocDelta(250)
                .cyclomaticComplexity(12)
                .status("PENDING")
                .build();

        when(reportRepository.findById(submissionId)).thenReturn(Optional.of(existingReport));

        kafkaConsumerService.consumeRiskScored(payload);

        ArgumentCaptor<RiskReport> reportCaptor = ArgumentCaptor.forClass(RiskReport.class);
        verify(reportRepository, times(1)).save(reportCaptor.capture());

        RiskReport savedReport = reportCaptor.getValue();
        assertEquals("High", savedReport.getRiskLevel());
        assertEquals(0.92, savedReport.getConfidence());
        assertEquals("COMPLETED", savedReport.getStatus()); // successfully completed!
        assertNotNull(savedReport.getGeneratedAt());
        assertFalse(savedReport.getRecommendations().isEmpty());
    }

    @Test
    public void testOutOfOrderMerging_CompletesSuccessfully() throws Exception {
        String submissionId = "sub-123";
        
        // 1. Consume risk.scored first (e.g. before metrics arrive)
        RiskScoredEvent scoredEvent = RiskScoredEvent.builder()
                .submissionId(submissionId)
                .riskLevel("Medium")
                .confidence(0.85)
                .contributors(List.of("Elevated complexity"))
                .build();
        String scoredPayload = objectMapper.writeValueAsString(scoredEvent);

        when(reportRepository.findById(submissionId)).thenReturn(Optional.empty());

        kafkaConsumerService.consumeRiskScored(scoredPayload);

        ArgumentCaptor<RiskReport> reportCaptor1 = ArgumentCaptor.forClass(RiskReport.class);
        verify(reportRepository, times(1)).save(reportCaptor1.capture());
        RiskReport savedReport1 = reportCaptor1.getValue();
        assertEquals("Medium", savedReport1.getRiskLevel());
        assertEquals("PENDING", savedReport1.getStatus()); // remains PENDING as metrics are missing

        // Reset mockito verification
        Mockito.reset(reportRepository);

        // 2. Consume change.enriched now
        ChangeEnrichedEvent enrichedEvent = ChangeEnrichedEvent.builder()
                .submissionId(submissionId)
                .totalLocDelta(120)
                .cyclomaticComplexity(8)
                .fileChurnRate(0.2)
                .sourceService("user-service")
                .impactedServices(List.of())
                .build();
        String enrichedPayload = objectMapper.writeValueAsString(enrichedEvent);

        // Mock report find returning the partial report saved in step 1
        when(reportRepository.findById(submissionId)).thenReturn(Optional.of(savedReport1));

        kafkaConsumerService.consumeChangeEnriched(enrichedPayload);

        ArgumentCaptor<RiskReport> reportCaptor2 = ArgumentCaptor.forClass(RiskReport.class);
        verify(reportRepository, times(1)).save(reportCaptor2.capture());
        
        RiskReport savedReport2 = reportCaptor2.getValue();
        assertEquals(120, savedReport2.getTotalLocDelta());
        assertEquals("Medium", savedReport2.getRiskLevel());
        assertEquals("COMPLETED", savedReport2.getStatus()); // completed on metrics merge!
        assertNotNull(savedReport2.getGeneratedAt());
        assertFalse(savedReport2.getRecommendations().isEmpty());
    }
}
