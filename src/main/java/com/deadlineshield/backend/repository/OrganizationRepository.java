package com.deadlineshield.backend.repository;

import com.deadlineshield.backend.model.Organization;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
