package com.eav02.backend.interaction.entity;

import java.time.Instant;
import java.util.UUID;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.project.entity.Project;
import com.eav02.backend.post.entity.Post;
import jakarta.persistence.*;

@Entity
@Table(name = "reactions", schema = "interactions")
public class Reaction {
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReactionType type;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    protected Reaction() {}
}
