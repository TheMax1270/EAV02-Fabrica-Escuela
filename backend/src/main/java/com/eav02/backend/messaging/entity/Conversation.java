package com.eav02.backend.messaging.entity;

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
@Table(name = "conversations", schema = "messaging")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_one_id", nullable = false, updatable = false)
    private AppUser userOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_two_id", nullable = false, updatable = false)
    private AppUser userTwo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    protected Conversation() {
    }

    public Conversation(AppUser userOne, AppUser userTwo, Instant createdAt) {
        this.userOne = userOne;
        this.userTwo = userTwo;
        this.createdAt = createdAt;
        this.lastMessageAt = createdAt;
    }

    public AppUser participantOtherThan(UUID userId) {
        return userOne.getId().equals(userId) ? userTwo : userOne;
    }

    public void registerMessage(Instant sentAt) {
        if (sentAt.isAfter(lastMessageAt)) {
            lastMessageAt = sentAt;
        }
    }

    public UUID getId() { return id; }
    public AppUser getUserOne() { return userOne; }
    public AppUser getUserTwo() { return userTwo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastMessageAt() { return lastMessageAt; }
}
