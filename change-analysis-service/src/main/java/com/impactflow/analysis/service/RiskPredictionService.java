
package com.impactflow.analysis.service;

import com.impactflow.analysis.dto.RiskPredictionResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class RiskPredictionService {

    private final RestClient restClient;

    public RiskPredictionService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:8000")
                .build();
    }

    public RiskPredictionResponse predict(
            int changedFiles,
            int linesAdded,
            int linesDeleted,
            int complexity) {

        Map<String, Object> request = new HashMap<>();
        request.put("changed_files", changedFiles);
        request.put("lines_added", linesAdded);
        request.put("lines_deleted", linesDeleted);
        request.put("complexity", complexity);

        return restClient.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RiskPredictionResponse.class);
    }
}
