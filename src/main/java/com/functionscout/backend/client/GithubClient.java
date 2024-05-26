package com.functionscout.backend.client;

import com.functionscout.backend.dto.GithubCreateIssueRequest;
import com.functionscout.backend.dto.GithubPomFileContentResponse;
import com.functionscout.backend.dto.GithubRepositoryContentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "Github-client", url = "${functionscout.github.client.base-url}")
public interface GithubClient {

    @GetMapping("/{owner}/{repo}/contents")
    List<GithubRepositoryContentResponse> getRepositoryContent(
            @RequestHeader("Authorization") String authToken,
            @RequestHeader("X-GitHub-Api-Version") String githubApiVersion,
            @RequestHeader("Accept") String acceptHeader,
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repository
    );

    @GetMapping("/{owner}/{repo}/contents/{file}")
    GithubPomFileContentResponse getRepositoryContentForFile(
            @RequestHeader("Authorization") String authToken,
            @RequestHeader("X-GitHub-Api-Version") String githubApiVersion,
            @RequestHeader("Accept") String acceptHeader,
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repository,
            @PathVariable("file") String file
    );

    @PostMapping("/{owner}/{repo}/issues")
    ResponseEntity<Object> createIssueOnRepository(
            @RequestHeader("Authorization") String authToken,
            @RequestHeader("X-GitHub-Api-Version") String githubApiVersion,
            @RequestHeader("Accept") String acceptHeader,
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repository,
            @RequestBody GithubCreateIssueRequest githubCreateIssueRequest
    );
}
