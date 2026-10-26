package com.impactflow.analysis.repository;

import com.impactflow.analysis.domain.ChangeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeSubmissionRepository extends JpaRepository<ChangeSubmission,String> {
}