package com.impactflow.analysis.dto;

import com.impactflow.analysis.domain.ChangeSubmission;

public class ChangeSubmittedEvent {

    private ChangeSubmission submission;

    public ChangeSubmittedEvent() {
    }

    public ChangeSubmittedEvent(ChangeSubmission submission) {
        this.submission = submission;
    }

    public ChangeSubmission getSubmission() {
        return submission;
    }

    public void setSubmission(ChangeSubmission submission) {
        this.submission = submission;
    }
}