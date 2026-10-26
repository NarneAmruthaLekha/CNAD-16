
package com.impactflow.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ReportKafkaConsumer {

    private final ReportStore reportStore;
    private final ObjectMapper objectMapper;

    public ReportKafkaConsumer(
            ReportStore reportStore,
            ObjectMapper objectMapper) {
        this.reportStore = reportStore;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "change.submitted",
            groupId = "report-service-group"
    )
    public void consume(String message) {
        try {
            Map<String, Object> root = objectMapper.readValue(
                    message,
                    new TypeReference<Map<String, Object>>() {}
            );

            Object value = root.get("submission");

            if (!(value instanceof Map<?, ?> raw)) {
                throw new IllegalArgumentException(
                        "Kafka event does not contain a submission");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> submission =
                    (Map<String, Object>) raw;

            Object idValue = submission.get("id");

            if (idValue == null || idValue.toString().isBlank()) {
                throw new IllegalArgumentException(
                        "Submission ID is missing");
            }

            submission.put("status", "COMPLETED");
            reportStore.add(idValue.toString(), submission);

            System.out.println(
                    "Report processed successfully: " + idValue);

        } catch (Exception e) {
            System.err.println(
                    "Failed to process report: " + e.getMessage());
        }
    }
}
