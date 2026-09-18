package com.eav02.backend.profile.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "developer_profiles")
public class DeveloperProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, length = 500)
    private String biography;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "programming_languages", nullable = false, columnDefinition = "text[]")
    private List<String> programmingLanguages = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> technologies = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 20)
    private ExperienceLevel experienceLevel;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "portfolio_url", length = 500)
    private String portfolioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeveloperProfile() {
    }

    public DeveloperProfile(AppUser user, String biography, List<String> programmingLanguages,
            List<String> technologies, ExperienceLevel experienceLevel, String githubUrl,
            String linkedinUrl, String portfolioUrl, Instant createdAt) {
        this.user = user;
        this.biography = biography;
        this.programmingLanguages = new ArrayList<>(programmingLanguages);
        this.technologies = new ArrayList<>(technologies);
        this.experienceLevel = experienceLevel;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.portfolioUrl = portfolioUrl;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(String biography, List<String> programmingLanguages, List<String> technologies,
            ExperienceLevel experienceLevel, String githubUrl, String linkedinUrl, String portfolioUrl,
            Instant updatedAt) {
        this.biography = biography;
        this.programmingLanguages = new ArrayList<>(programmingLanguages);
        this.technologies = new ArrayList<>(technologies);
        this.experienceLevel = experienceLevel;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.portfolioUrl = portfolioUrl;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getBiography() { return biography; }
    public List<String> getProgrammingLanguages() { return List.copyOf(programmingLanguages); }
    public List<String> getTechnologies() { return List.copyOf(technologies); }
    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public String getGithubUrl() { return githubUrl; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
