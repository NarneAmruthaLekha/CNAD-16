package com.impactflow.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSubmittedEvent {
    private ChangeSubmissionDto submission;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeSubmissionDto {
        private String id;
        private String repoUrl;
        private String commitId;
        private String branch;
        private LocalDateTime submittedAt;
        private String status;
        private List<String> changedFiles;
    }
}
