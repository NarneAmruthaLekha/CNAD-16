package com.impactflow.report.repository;

import com.impactflow.report.domain.RiskReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<RiskReport, String> {
}
