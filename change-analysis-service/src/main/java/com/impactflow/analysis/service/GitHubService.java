
package com.impactflow.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> analyzeCommit(String repoUrl, String commitId)
            throws IOException, InterruptedException {
        String path = repoPath(repoUrl);

        if (commitId == null || !commitId.matches("[a-fA-F0-9]{7,40}")) {
            throw new IllegalArgumentException("Invalid commit SHA");
        }

        String url = "https://api.github.com/repos/" + path
                + "/commits/" + commitId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ImpactFlow")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            System.out.println("GitHub API URL: " + url);
            System.out.println("GitHub API response: " + response.body());
            throw new IllegalArgumentException(
                    "GitHub API returned 404: " + response.body());
        }

        if (response.statusCode() == 403 || response.statusCode() == 429) {
            throw new IllegalStateException(
                    "GitHub API rate limit reached. Try again later.");
        }

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "GitHub API returned HTTP " + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode filesNode = root.path("files");

        if (!filesNode.isArray()) {
            throw new IllegalStateException(
                    "GitHub did not return commit file details");
        }

        List<String> files = new ArrayList<>();
        Set<String> changedFiles = new HashSet<>();
        int added = 0;
        int deleted = 0;
        int complexity = 1;

        for (JsonNode file : filesNode) {
            String filename = file.path("filename").asText();
            files.add(filename);
            changedFiles.add(filename);

            added += file.path("additions").asInt(0);
            deleted += file.path("deletions").asInt(0);

            String patch = file.path("patch").asText("");
            Matcher matcher = Pattern.compile(
                    "\\b(if|else\\s+if|for|while|case|catch)\\b|&&|\\|\\||\\?")
                    .matcher(patch);

            while (matcher.find()) {
                complexity++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("repoUrl", "https://github.com/" + path);
        result.put("commitId", root.path("sha").asText(commitId));
        result.put("commitMessage",
                root.path("commit").path("message").asText(""));
        result.put("branch", "");
        result.put("changedFiles", files);
        result.put("changedFilesCount", changedFiles.size());
        result.put("linesAdded", added);
        result.put("linesDeleted", deleted);
        result.put("complexity", Math.min(complexity, 5));

        return result;
    }

    private String repoPath(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("Repository URL is required");
        }

        String value = repoUrl.trim()
                .replaceFirst("/+$", "")
                .replaceFirst("\\.git$", "");

        Matcher matcher = Pattern.compile(
                "^https://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$")
                .matcher(value);

        if (!matcher.matches()
                || matcher.group(1).equals(".")
                || matcher.group(1).equals("..")
                || matcher.group(2).equals(".")
                || matcher.group(2).equals("..")) {
            throw new IllegalArgumentException(
                    "Enter a valid GitHub repository URL");
        }

        return matcher.group(1) + "/" + matcher.group(2);
    }
}
