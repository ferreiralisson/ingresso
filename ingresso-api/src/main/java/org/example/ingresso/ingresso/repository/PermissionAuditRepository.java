package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.PermissionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionAuditRepository extends JpaRepository<PermissionAudit, Long> {
}