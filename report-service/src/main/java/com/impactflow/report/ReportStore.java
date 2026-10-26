
package com.impactflow.report;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReportStore {

    private final ReportRepository reportRepository;

    public ReportStore(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public synchronized void add(String id, Map<String, Object> report) {
        reportRepository.save(new ReportData(id, report));
    }

    public synchronized List<ReportData> getAll() {
        return reportRepository.findAll();
    }

    public synchronized Optional<ReportData> getById(String id) {
        return reportRepository.findById(id);
    }
}
