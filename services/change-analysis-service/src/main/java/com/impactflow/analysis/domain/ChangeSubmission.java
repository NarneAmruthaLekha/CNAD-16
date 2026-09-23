package com.impactflow.analysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "change_submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSubmission {

    @Id
    private String id; // UUID

    @Column(nullable = false)
    private String repoUrl;

    @Column(nullable = false)
    private String commitId;

    private String branch;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private String status; // PENDING, ANALYZED, FAILED

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "change_files", joinColumns = @JoinColumn(name = "change_id"))
    @Column(name = "file_path")
    @Builder.Default
    private List<String> changedFiles = new ArrayList<>();
}
