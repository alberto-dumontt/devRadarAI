package albertodumontt.devRadarAI.infrastructure.controller;

import albertodumontt.devRadarAI.infrastructure.crawler.LinkedinWorker;
import albertodumontt.devRadarAI.model.JobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Developer jobs from LinkedIn")
public class JobController {

    private final LinkedinWorker linkedinWorker;

    @GetMapping
    @Operation(summary = "Crawls LinkedIn for developer jobs (Brazil, remote, last 7 days)")
    public ResponseEntity<List<JobResponse>> findAll() {
        return ResponseEntity.ok(linkedinWorker.crawl());
    }
}
