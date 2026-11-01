package com.deadlineshield.backend.service;

import com.deadlineshield.backend.dto.ClientApprovalRequest;
import com.deadlineshield.backend.dto.PauseProjectRequest;
import com.deadlineshield.backend.model.AuditLog;
import com.deadlineshield.backend.model.Milestone;
import com.deadlineshield.backend.model.MilestoneStatus;
import com.deadlineshield.backend.model.Project;
import com.deadlineshield.backend.exception.ResourceNotFoundException;
import com.deadlineshield.backend.repository.AuditLogRepository;
import com.deadlineshield.backend.repository.MilestoneRepository;
import com.deadlineshield.backend.repository.ProjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectClockService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectClockService.class);

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    @Autowired
    public ProjectClockService(
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            AuditLogRepository auditLogRepository) {
        this(projectRepository, milestoneRepository, auditLogRepository, Clock.systemUTC());
    }

    ProjectClockService(
            ProjectRepository projectRepository,
            MilestoneRepository milestoneRepository,
            AuditLogRepository auditLogRepository,
            Clock clock) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Transactional
    public Milestone pauseClock(PauseProjectRequest request) {
        if (request == null || request.milestoneId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A milestone ID is required");
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pause reason must not be blank");
        }
        Milestone milestone = findMilestone(request.milestoneId());
        if (!milestone.isClockPaused()) {
            pause(milestone, request.reason().trim());
            milestoneRepository.save(milestone);
        }
        logger.info("Milestone clock paused: milestone {}", milestone.getId());
        return milestone;
    }

    @Transactional
    public Milestone resumeClock(UUID token, ClientApprovalRequest request) {
        if (request == null || token == null) {
            throw invalidReviewLink();
        }

        Milestone milestone = milestoneRepository.findByToken(token)
                .orElseThrow(this::invalidReviewLink);
        if (!milestone.isClockPaused() || milestone.getReviewedAt() != null) {
            throw invalidReviewLink();
        }
        if (milestone.getPausedAt() == null) {
            throw invalidReviewLink();
        }
        if (request.feedback() == null || request.feedback().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review feedback must not be blank");
        }

        Instant reviewedAt = clock.instant();
        long pausedDays = ChronoUnit.DAYS.between(milestone.getPausedAt(), reviewedAt);
        Project project = milestone.getProject();
        if (project.getAdjustedTargetDate() != null && pausedDays > 0) {
            project.setAdjustedTargetDate(project.getAdjustedTargetDate().plus(pausedDays, ChronoUnit.DAYS));
            projectRepository.save(project);
        }
        milestone.setClockPaused(false);
        milestone.setReviewFeedback(request.feedback().trim());
        milestone.setReviewedAt(reviewedAt);
        milestone.setStatus(MilestoneStatus.APPROVED);
        milestoneRepository.save(milestone);
        recordAudit(
                "MILESTONE_REVIEWED",
                milestone,
                "Client review submitted; clock resumed after " + pausedDays + " paused days");
        logger.info("Milestone review accepted and clock resumed: milestone {}", milestone.getId());
        return milestone;
    }

    @Transactional
    public int pauseProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        int pausedCount = 0;
        for (Milestone milestone : milestoneRepository.findAllByProjectId(projectId)) {
            if (!milestone.isClockPaused()) {
                pause(milestone, "Project paused by request");
                milestoneRepository.save(milestone);
                pausedCount++;
            }
        }
        logger.info("Paused {} milestone clocks for project {}", pausedCount, project.getId());
        return pausedCount;
    }

    private Milestone findMilestone(UUID milestoneId) {
        return milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));
    }

    private void pause(Milestone milestone, String reason) {
        if (!milestone.isClockPaused()) {
            milestone.setClockPaused(true);
            milestone.setPausedAt(clock.instant());
            recordAudit("MILESTONE_CLOCK_PAUSED", milestone, reason);
        }
    }

    private ResourceNotFoundException invalidReviewLink() {
        logger.warn("Rejected invalid, inactive, or previously used client review link");
        return new ResourceNotFoundException("Review link is invalid or no longer available");
    }

    private void recordAudit(String action, Milestone milestone, String details) {
        recordAudit(action, milestone.getProject(), milestone, details);
    }

    private void recordAudit(String action, Project project, Milestone milestone, String details) {
        auditLogRepository.save(new AuditLog(action, details, project, milestone));
    }
}
