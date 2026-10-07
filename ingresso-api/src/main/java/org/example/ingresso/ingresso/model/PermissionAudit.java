package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.PermissionAuditAction;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "permission_audits")
public class PermissionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_email", nullable = false)
    private String targetEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UsuarioPerfil permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PermissionAuditAction action;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PermissionAudit() {
    }

    public PermissionAudit(
        Long actorUserId,
        String actorEmail,
        Long targetUserId,
        String targetEmail,
        UsuarioPerfil permission,
        PermissionAuditAction action,
        Instant occurredAt
    ) {
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.targetUserId = targetUserId;
        this.targetEmail = targetEmail;
        this.permission = permission;
        this.action = action;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public UsuarioPerfil getPermission() {
        return permission;
    }

    public PermissionAuditAction getAction() {
        return action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}