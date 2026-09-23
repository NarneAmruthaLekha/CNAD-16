package com.impactflow.analysis.service;

import com.impactflow.analysis.domain.DependencyNode;
import com.impactflow.analysis.repository.DependencyNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DependencyGraphManager {

    @Autowired
    private DependencyNodeRepository nodeRepository;

    public void registerDependency(String serviceName, List<String> dependencies) {
        DependencyNode node = nodeRepository.findByServiceName(serviceName)
                .orElse(DependencyNode.builder().serviceName(serviceName).build());

        // Update dependencies
        node.setDependencies(dependencies != null ? dependencies : new ArrayList<>());
        nodeRepository.save(node);

        // Update target dependents
        if (dependencies != null) {
            for (String dep : dependencies) {
                DependencyNode depNode = nodeRepository.findByServiceName(dep)
                        .orElse(DependencyNode.builder().serviceName(dep).build());
                if (!depNode.getDependents().contains(serviceName)) {
                    depNode.getDependents().add(serviceName);
                    nodeRepository.save(depNode);
                }
            }
        }
    }

    public List<String> calculateImpactedServices(String serviceName) {
        List<String> impacted = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Start BFS from target service's dependents (downstream services impacted when this service changes)
        Optional<DependencyNode> rootOpt = nodeRepository.findByServiceName(serviceName);
        if (rootOpt.isEmpty()) {
            return impacted;
        }

        DependencyNode root = rootOpt.get();
        for (String dep : root.getDependents()) {
            queue.add(dep);
            visited.add(dep);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            impacted.add(current);

            Optional<DependencyNode> nodeOpt = nodeRepository.findByServiceName(current);
            if (nodeOpt.isPresent()) {
                for (String childDependent : nodeOpt.get().getDependents()) {
                    if (!visited.contains(childDependent)) {
                        visited.add(childDependent);
                        queue.add(childDependent);
                    }
                }
            }
        }

        return impacted;
    }

    public List<DependencyNode> getAllNodes() {
        return nodeRepository.findAll();
    }
}
