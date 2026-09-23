package com.impactflow.analysis.dto;

import com.impactflow.analysis.domain.ChangeSubmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSubmittedEvent {
    private ChangeSubmission submission;
}
