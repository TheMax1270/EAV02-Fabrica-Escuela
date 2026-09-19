package com.eav02.backend.profile.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.eav02.backend.profile.entity.DeveloperProfile;
import com.eav02.backend.profile.entity.ExperienceLevel;

public record ProfileResponse(UUID id, UUID userId, String username, String biography,
        List<String> programmingLanguages, List<String> technologies, ExperienceLevel experienceLevel,
        String githubUrl, String linkedinUrl, String portfolioUrl, Instant createdAt, Instant updatedAt) {

    public static ProfileResponse from(DeveloperProfile profile) {
        return new ProfileResponse(profile.getId(), profile.getUser().getId(), profile.getUser().getUsername(),
                profile.getBiography(), profile.getProgrammingLanguages(), profile.getTechnologies(),
                profile.getExperienceLevel(), profile.getGithubUrl(), profile.getLinkedinUrl(),
                profile.getPortfolioUrl(), profile.getCreatedAt(), profile.getUpdatedAt());
    }
}
