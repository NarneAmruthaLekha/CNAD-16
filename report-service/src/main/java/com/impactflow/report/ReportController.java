
package com.impactflow.report;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportStore reportStore;

    public ReportController(ReportStore reportStore) {
        this.reportStore = reportStore;
    }

    @GetMapping("/health")
    public String health() {
        return "ImpactFlow Report Service is running!";
    }

    @GetMapping
    public List<ReportData> getAllReports() {
        return reportStore.getAll();
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        List<ReportData> reports = reportStore.getAll();
        long low = 0, medium = 0, high = 0;

        for (ReportData r : reports) {
            Object risk = r.getReport().get("risk");

            if (risk != null) {
                switch (risk.toString().toUpperCase()) {
                    case "LOW" -> low++;
                    case "MEDIUM" -> medium++;
                    case "HIGH" -> high++;
                }
            }
        }

        return Map.of(
            "totalReports", reports.size(),
            "lowRisk", low,
            "mediumRisk", medium,
            "highRisk", high
        );
    }

    @GetMapping("/{id}")
    public ReportData getReport(@PathVariable String id) {
        return reportStore.getById(id).orElse(null);
    }
}
