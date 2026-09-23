package com.impactflow.analysis.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dependency_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serviceName;

    private String moduleName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "node_dependencies", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "dependency_service")
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "node_dependents", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "dependent_service")
    @Builder.Default
    private List<String> dependents = new ArrayList<>();
}
