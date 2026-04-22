package com.derwin.prepforge.jobs;

import com.derwin.prepforge.jobs.dto.AsyncJobStatusResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final AsyncJobService asyncJobService;

    @GetMapping("/{jobId}")
    public ResponseEntity<AsyncJobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(asyncJobService.getJobStatus(jobId));
    }
}
