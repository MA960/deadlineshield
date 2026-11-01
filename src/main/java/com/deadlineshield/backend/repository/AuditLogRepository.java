package com.deadlineshield.backend.repository;

import com.deadlineshield.backend.model.AuditLog;
import org.springframework.data.repository.Repository;

public interface AuditLogRepository extends Repository<AuditLog, java.util.UUID> {

    AuditLog save(AuditLog auditLog);
}
