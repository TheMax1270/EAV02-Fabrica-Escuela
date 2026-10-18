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
@Table(name = "user_follows", schema = "interactions")
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private AppUser follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followed_id", nullable = false)
    private AppUser followed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserFollow() {
    }

    public UUID getId() {
        return id;
    }

    public AppUser getFollower() {
        return follower;
    }

    public AppUser getFollowed() {
        return followed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
