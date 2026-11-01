package com.deadlineshield.backend.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuditLogImmutabilityTest {

    @Test
    void preventsUpdatingAnAuditEntry() {
        AuditLog auditLog = new AuditLog("MILESTONE_CLOCK_PAUSED", "Waiting for feedback", null, null);

        assertThrows(IllegalStateException.class, auditLog::preventUpdate);
    }

    @Test
    void preventsDeletingAnAuditEntry() {
        AuditLog auditLog = new AuditLog("MILESTONE_CLOCK_PAUSED", "Waiting for feedback", null, null);

        assertThrows(IllegalStateException.class, auditLog::preventRemoval);
    }
}
