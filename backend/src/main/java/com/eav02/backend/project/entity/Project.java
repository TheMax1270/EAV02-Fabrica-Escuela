package com.eav02.backend.project.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.eav02.backend.user.entity.AppUser;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects", schema = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> technologies = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status;

    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
    }

    public Project(AppUser user, String title, String description, List<String> technologies,
            ProjectStatus status, String repositoryUrl, Instant createdAt) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.technologies = new ArrayList<>(technologies);
        this.status = status;
        this.repositoryUrl = repositoryUrl;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(String title, String description, List<String> technologies, ProjectStatus status,
            String repositoryUrl, Instant updatedAt) {
        this.title = title;
        this.description = description;
        this.technologies = new ArrayList<>(technologies);
        this.status = status;
        this.repositoryUrl = repositoryUrl;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public List<String> getTechnologies() { return List.copyOf(technologies); }
    public ProjectStatus getStatus() { return status; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}