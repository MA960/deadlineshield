package com.deadlineshield.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deadlineshield.backend.dto.ClientApprovalRequest;
import com.deadlineshield.backend.model.AuditLog;
import com.deadlineshield.backend.model.Milestone;
import com.deadlineshield.backend.model.MilestoneStatus;
import com.deadlineshield.backend.model.Project;
import com.deadlineshield.backend.repository.AuditLogRepository;
import com.deadlineshield.backend.repository.MilestoneRepository;
import com.deadlineshield.backend.repository.ProjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectClockServiceTest {

    private static final Instant REVIEWED_AT = Instant.parse("2026-11-01T10:00:00Z");

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private ProjectClockService projectClockService;

    @BeforeEach
    void setUp() {
        projectClockService = new ProjectClockService(
                projectRepository,
                milestoneRepository,
                auditLogRepository,
                Clock.fixed(REVIEWED_AT, ZoneOffset.UTC));
    }

    @Test
    void resumeClockExtendsAdjustedTargetDateByPausedDaysApprovesMilestoneAndSavesAuditLog() {
        UUID token = UUID.randomUUID();
        Instant originalAdjustedTargetDate = Instant.parse("2026-12-01T00:00:00Z");

        Project project = new Project();
        project.setAdjustedTargetDate(originalAdjustedTargetDate);

        Milestone milestone = new Milestone();
        milestone.setProject(project);
        milestone.setClockPaused(true);
        milestone.setPausedAt(REVIEWED_AT.minusSeconds(5L * 24 * 60 * 60 + 3600));
        when(milestoneRepository.findByToken(token)).thenReturn(java.util.Optional.of(milestone));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Milestone result = projectClockService.resumeClock(token, new ClientApprovalRequest("Looks good"));

        assertEquals(originalAdjustedTargetDate.plusSeconds(5L * 24 * 60 * 60),
                project.getAdjustedTargetDate());
        assertEquals(MilestoneStatus.APPROVED, result.getStatus());
        assertFalse(result.isClockPaused());
        assertEquals("Looks good", result.getReviewFeedback());
        assertEquals(REVIEWED_AT, result.getReviewedAt());

        verify(projectRepository).save(project);
        verify(milestoneRepository).save(milestone);
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertEquals("MILESTONE_REVIEWED", auditLogCaptor.getValue().getAction());
        assertEquals("Client review submitted; clock resumed after 5 paused days",
                auditLogCaptor.getValue().getDetails());
        assertEquals(project, auditLogCaptor.getValue().getProject());
        assertEquals(milestone, auditLogCaptor.getValue().getMilestone());
    }
}
