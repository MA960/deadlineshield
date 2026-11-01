package com.deadlineshield.backend.controller;

import com.deadlineshield.backend.dto.ClientApprovalRequest;
import com.deadlineshield.backend.dto.PauseProjectRequest;
import com.deadlineshield.backend.model.Milestone;
import com.deadlineshield.backend.model.Project;
import com.deadlineshield.backend.repository.ProjectRepository;
import com.deadlineshield.backend.service.ProjectClockService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectClockService projectClockService;
    private final ProjectRepository projectRepository;

    public ProjectController(
            ProjectClockService projectClockService,
            ProjectRepository projectRepository) {
        this.projectClockService = projectClockService;
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public ResponseEntity<List<Project>> getProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @PostMapping("/{projectId}/pause")
    public ResponseEntity<PauseProjectResponse> pauseProject(@PathVariable UUID projectId) {
        int pausedMilestones = projectClockService.pauseProject(projectId);
        logger.info("Project pause request completed for project {}: {} milestone clocks paused",
                projectId, pausedMilestones);
        return ResponseEntity.ok(new PauseProjectResponse(projectId, pausedMilestones));
    }

    @PostMapping("/milestones/pause")
    public ResponseEntity<PauseMilestoneResponse> pauseMilestone(
            @Valid @RequestBody PauseProjectRequest request) {
        Milestone milestone = projectClockService.pauseClock(request);
        logger.info("Milestone clock pause request completed for milestone {}", milestone.getId());
        return ResponseEntity.ok(new PauseMilestoneResponse(
                milestone.getId(),
                milestone.isClockPaused(),
                milestone.getPausedAt()));
    }

    @PostMapping("/reviews/{token}")
    public ResponseEntity<ReviewResponse> submitClientReview(
            @PathVariable UUID token,
            @Valid @RequestBody ClientApprovalRequest request) {
        Milestone milestone = projectClockService.resumeClock(token, request);
        logger.info("Client review accepted for milestone {}", milestone.getId());
        return ResponseEntity.ok(new ReviewResponse(
                milestone.getId(),
                milestone.isClockPaused(),
                milestone.getReviewFeedback(),
                milestone.getReviewedAt()));
    }

    public record PauseProjectResponse(UUID projectId, int pausedMilestones) {
    }

    public record PauseMilestoneResponse(UUID milestoneId, boolean clockPaused, Instant pausedAt) {
    }

    public record ReviewResponse(
            UUID milestoneId,
            boolean clockPaused,
            String feedback,
            Instant reviewedAt) {
    }
}
