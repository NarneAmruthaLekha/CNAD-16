package com.impactflow.report.controller;

import com.impactflow.report.domain.RiskReport;
import com.impactflow.report.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportRepository reportRepository;

    @GetMapping
    public ResponseEntity<List<RiskReport>> getAllReports() {
        logger.info("Fetching all reports");
        // Sort by submittedAt descending to show recent changes first
        List<RiskReport> reports = reportRepository.findAll(Sort.by(Sort.Direction.DESC, "submittedAt"));
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskReport> getReportById(@PathVariable("id") String id) {
        logger.info("Fetching report for ID: {}", id);
        Optional<RiskReport> reportOpt = reportRepository.findById(id);
        return reportOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable("id") String id) {
        logger.info("Deleting report for ID: {}", id);
        if (reportRepository.existsById(id)) {
            reportRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
