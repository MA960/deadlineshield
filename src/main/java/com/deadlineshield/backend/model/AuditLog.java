package com.deadlineshield.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String action;

    private String details;

    private Instant occurredAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    private Milestone milestone;

    public AuditLog(String action, String details, Project project, Milestone milestone) {
        this.action = action;
        this.details = details;
        this.project = project;
        this.milestone = milestone;
    }

    @PreUpdate
    void preventUpdate() {
        throw new IllegalStateException("Audit log entries are immutable");
    }

    @PreRemove
    void preventRemoval() {
        throw new IllegalStateException("Audit log entries cannot be deleted");
    }
}
