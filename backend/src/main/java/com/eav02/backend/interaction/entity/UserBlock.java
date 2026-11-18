package com.eav02.backend.interaction.entity;

import java.time.Instant;
import java.util.UUID;

import com.eav02.backend.user.entity.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_blocks", schema = "interactions")
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private AppUser blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private AppUser blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserBlock() {
    }

    public UserBlock(AppUser blocker, AppUser blocked, Instant createdAt) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public AppUser getBlocker() { return blocker; }
    public AppUser getBlocked() { return blocked; }
    public Instant getCreatedAt() { return createdAt; }
}
