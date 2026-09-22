package com.eav02.backend.profile.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eav02.backend.common.exception.ProfileAlreadyExistsException;
import com.eav02.backend.common.exception.ProfileNotFoundException;
import com.eav02.backend.common.exception.ProfileOwnershipException;
import com.eav02.backend.profile.dto.ProfileRequest;
import com.eav02.backend.profile.dto.ProfileResponse;
import com.eav02.backend.profile.entity.DeveloperProfile;
import com.eav02.backend.profile.repository.DeveloperProfileRepository;
import com.eav02.backend.user.repository.UserRepository;

@Service
public class ProfileService implements IProfileService {

    private final DeveloperProfileRepository profiles;
    private final UserRepository users;
    private final Clock clock;

    public ProfileService(DeveloperProfileRepository profiles, UserRepository users, Clock clock) {
        this.profiles = profiles;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public ProfileResponse create(UUID userId, ProfileRequest request) {
        if (profiles.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException();
        }
        var user = users.findById(userId).orElseThrow(ProfileNotFoundException::new);
        var profile = new DeveloperProfile(user, request.biography(), request.programmingLanguages(),
                request.technologies(), request.experienceLevel(), request.githubUrl(), request.linkedinUrl(),
                request.portfolioUrl(), clock.instant());
        return ProfileResponse.from(profiles.saveAndFlush(profile));
    }

    @Transactional(readOnly = true)
    public ProfileResponse getOwn(UUID userId) {
        return profiles.findByUserId(userId).map(ProfileResponse::from)
                .orElseThrow(ProfileNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getPublic(UUID profileId) {
        return profiles.findWithUserById(profileId).map(ProfileResponse::from)
                .orElseThrow(ProfileNotFoundException::new);
    }

    @Transactional
    public ProfileResponse updateOwn(UUID userId, ProfileRequest request) {
        var profile = profiles.findByUserId(userId).orElseThrow(ProfileNotFoundException::new);
        update(profile, request);
        return ProfileResponse.from(profile);
    }

    @Transactional
    public ProfileResponse update(UUID profileId, UUID userId, ProfileRequest request) {
        var profile = profiles.findWithUserById(profileId).orElseThrow(ProfileNotFoundException::new);
        if (!profile.getUser().getId().equals(userId)) {
            throw new ProfileOwnershipException();
        }
        update(profile, request);
        return ProfileResponse.from(profile);
    }

    private void update(DeveloperProfile profile, ProfileRequest request) {
        profile.update(request.biography(), request.programmingLanguages(), request.technologies(),
                request.experienceLevel(), request.githubUrl(), request.linkedinUrl(), request.portfolioUrl(),
                clock.instant());
    }
}
