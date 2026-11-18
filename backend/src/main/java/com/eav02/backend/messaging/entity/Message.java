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
@Table(name = "messages", schema = "messaging")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private AppUser sender;

    @Column(nullable = false, updatable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(Conversation conversation, AppUser sender, String content, Instant createdAt) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public AppUser getSender() { return sender; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
