package com.impactflow.report.controller;

import com.impactflow.report.domain.RiskReport;
import com.impactflow.report.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportControllerTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllReports() {
        List<RiskReport> mockReports = List.of(
                RiskReport.builder().id("r1").status("COMPLETED").build(),
                RiskReport.builder().id("r2").status("PENDING").build()
        );

        when(reportRepository.findAll(any(Sort.class))).thenReturn(mockReports);

        ResponseEntity<List<RiskReport>> response = reportController.getAllReports();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(reportRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    public void testGetReportById_Found() {
        RiskReport mockReport = RiskReport.builder().id("r1").status("COMPLETED").build();

        when(reportRepository.findById("r1")).thenReturn(Optional.of(mockReport));

        ResponseEntity<RiskReport> response = reportController.getReportById("r1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("r1", response.getBody().getId());
        verify(reportRepository, times(1)).findById("r1");
    }

    @Test
    public void testGetReportById_NotFound() {
        when(reportRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<RiskReport> response = reportController.getReportById("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(reportRepository, times(1)).findById("nonexistent");
    }

    @Test
    public void testDeleteReport_Success() {
        when(reportRepository.existsById("r1")).thenReturn(true);

        ResponseEntity<?> response = reportController.deleteReport("r1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(reportRepository, times(1)).deleteById("r1");
    }

    @Test
    public void testDeleteReport_NotFound() {
        when(reportRepository.existsById("nonexistent")).thenReturn(false);

        ResponseEntity<?> response = reportController.deleteReport("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(reportRepository, never()).deleteById("nonexistent");
    }
}
