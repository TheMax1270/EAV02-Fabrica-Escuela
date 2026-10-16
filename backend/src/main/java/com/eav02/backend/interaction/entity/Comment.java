package com.eav02.backend.interaction.entity;

import java.time.Instant;
import java.util.UUID;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.project.entity.Project;
import com.eav02.backend.post.entity.Post;
import com.eav02.backend.moderation.entity.ModerationStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "comments", schema = "interactions")
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.VISIBLE;

    protected Comment() {}
    public Comment(AppUser user, Project project, Post post, Comment parent, String content, Instant createdAt) {
        if ((project == null) == (post == null)) throw new IllegalArgumentException("El comentario requiere un unico destino");
        if (parent != null && (parent.getParent() != null
                || !java.util.Objects.equals(project == null ? null : project.getId(), parent.getProject() == null ? null : parent.getProject().getId())
                || !java.util.Objects.equals(post == null ? null : post.getId(), parent.getPost() == null ? null : parent.getPost().getId()))) {
            throw new IllegalArgumentException("El padre debe ser raiz y pertenecer al mismo contenido");
        }
        this.user = user;
        this.project = project;
        this.post = post;
        this.parent = parent;
        this.content = content;
        this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public Project getProject() { return project; }
    public Post getPost() { return post; }
    public Comment getParent() { return parent; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public ModerationStatus getModerationStatus() { return moderationStatus; }
    public void markInReview() { moderationStatus = ModerationStatus.IN_REVIEW; }
    public void restore() { moderationStatus = ModerationStatus.VISIBLE; }
    public void remove() { moderationStatus = ModerationStatus.REMOVED; }
}
