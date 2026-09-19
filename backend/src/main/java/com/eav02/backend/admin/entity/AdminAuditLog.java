package com.eav02.backend.admin.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.eav02.backend.user.entity.AppUser;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs", schema = "admin")
public class AdminAuditLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private AppUser admin;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminAuditLog() {
    }

    public AdminAuditLog(UUID id, AppUser admin, AdminAction action, String targetType, UUID targetId,
            Map<String, Object> details, Instant createdAt) {
        this.id = id;
        this.admin = admin;
        this.action = action.name();
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details == null ? null : Map.copyOf(details);
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public AppUser getAdmin() { return admin; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public Map<String, Object> getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
