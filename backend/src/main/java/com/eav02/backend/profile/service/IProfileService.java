package com.eav02.backend.profile.service;

import java.util.UUID;

import com.eav02.backend.profile.dto.ProfileRequest;
import com.eav02.backend.profile.dto.ProfileResponse;

public interface IProfileService {

    ProfileResponse create(UUID userId, ProfileRequest request);

    ProfileResponse getOwn(UUID userId);

    ProfileResponse getPublic(UUID profileId);

    ProfileResponse updateOwn(UUID userId, ProfileRequest request);

    ProfileResponse update(UUID profileId, UUID userId, ProfileRequest request);
}