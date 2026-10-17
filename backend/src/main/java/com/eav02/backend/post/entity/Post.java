package com.eav02.backend.post.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.moderation.entity.ModerationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts", schema = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostType type;

    @Column(length = 500)
    private String link;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> categories = new ArrayList<>();

    @Column(nullable = false)
    private boolean resolved;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.VISIBLE;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Post() {
    }

    public Post(AppUser user, String title, String content, Instant createdAt) {
        this(user, title, content, PostType.GENERAL, null, List.of(), createdAt);
    }

    public Post(AppUser user, String title, String content, PostType type, String link,
            List<String> categories, Instant createdAt) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.type = type;
        this.link = link;
        this.categories = new ArrayList<>(categories);
        this.resolved = false;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public PostType getType() { return type; }
    public String getLink() { return link; }
    public List<String> getCategories() { return List.copyOf(categories); }
    public boolean isResolved() { return resolved; }
    public ModerationStatus getModerationStatus() { return moderationStatus; }

    public void markInReview() { moderationStatus = ModerationStatus.IN_REVIEW; }
    public void restore() { moderationStatus = ModerationStatus.VISIBLE; }
    public void remove() { moderationStatus = ModerationStatus.REMOVED; }

    public void resolve() {
        if (type != PostType.TECHNICAL_DISCUSSION) {
            throw new com.eav02.backend.common.exception.PostTypeConflictException();
        }
        resolved = true;
    }
}
