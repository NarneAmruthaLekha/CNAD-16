package com.impactflow.analysis.repository;

import com.impactflow.analysis.domain.DependencyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DependencyNodeRepository extends JpaRepository<DependencyNode, Long> {
    Optional<DependencyNode> findByServiceName(String serviceName);
    boolean existsByServiceName(String serviceName);
}
