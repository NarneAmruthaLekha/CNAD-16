package com.impactflow.analysis.dto;

public class RiskPredictionResponse {

    private String risk;
    private double risk_score;
    private String message;

    public RiskPredictionResponse() {
    }

    public String getRisk() {
        return risk;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public double getRisk_score() {
        return risk_score;
    }

    public void setRisk_score(double risk_score) {
        this.risk_score = risk_score;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
