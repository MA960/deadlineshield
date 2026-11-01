package com.deadlineshield.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "milestones")
@Getter
@Setter
@NoArgsConstructor
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private Instant dueAt;

    @Column(nullable = false, unique = true, updatable = false)
    @JsonIgnore
    private UUID token = UUID.randomUUID();

    @Column(nullable = false)
    private boolean clockPaused;

    private Instant pausedAt;

    private String reviewFeedback;

    private Instant reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status = MilestoneStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    private Project project;
}
