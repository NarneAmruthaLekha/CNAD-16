
package com.impactflow.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Entity
@Table(name = "reports")
public class ReportData {

    @Id
    private String id;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String reportJson;

    public ReportData() {
    }

    public ReportData(String id, Map<String, Object> report) {
        this.id = id;
        try {
            this.reportJson = new ObjectMapper().writeValueAsString(report);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize report", e);
        }
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getReport() {
        try {
            return new ObjectMapper().readValue(
                reportJson,
                new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize report", e);
        }
    }
}
