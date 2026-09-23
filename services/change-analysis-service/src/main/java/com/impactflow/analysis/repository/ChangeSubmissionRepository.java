package com.impactflow.analysis.repository;

import com.impactflow.analysis.domain.ChangeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChangeSubmissionRepository extends JpaRepository<ChangeSubmission, String> {
}
