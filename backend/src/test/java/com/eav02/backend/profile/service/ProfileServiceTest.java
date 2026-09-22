package com.eav02.backend.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.eav02.backend.common.exception.ProfileAlreadyExistsException;
import com.eav02.backend.common.exception.ProfileNotFoundException;
import com.eav02.backend.common.exception.ProfileOwnershipException;
import com.eav02.backend.profile.dto.ProfileRequest;
import com.eav02.backend.profile.entity.DeveloperProfile;
import com.eav02.backend.profile.entity.ExperienceLevel;
import com.eav02.backend.profile.repository.DeveloperProfileRepository;
import com.eav02.backend.user.entity.AppUser;
import com.eav02.backend.user.entity.UserRole;
import com.eav02.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private DeveloperProfileRepository profiles;

    @Mock
    private UserRepository users;

    @InjectMocks
    private ProfileService service;

    private AppUser user;
    private ProfileRequest request;

    @BeforeEach
    void setUp() {
        service = new ProfileService(profiles, users, Clock.fixed(NOW, ZoneOffset.UTC));
        user = new AppUser("Ada Lovelace", "ada", "ada@example.com", "hash", UserRole.DEVELOPER, true, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        request = new ProfileRequest("Backend developer", List.of("Java", "SQL"), List.of("Spring"),
                ExperienceLevel.SEMI_SENIOR, "https://github.com/ada", null, null);
    }

    @Test
    void createsProfileWhenUserHasNoneYet() {
        UUID userId = UUID.randomUUID();
        when(profiles.existsByUserId(userId)).thenReturn(false);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(profiles.saveAndFlush(any(DeveloperProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(userId, request);

        assertEquals("Backend developer", response.biography());
        assertEquals(ExperienceLevel.SEMI_SENIOR, response.experienceLevel());
    }

    @Test
    void rejectsSecondProfileForSameUser() {
        UUID userId = UUID.randomUUID();
        when(profiles.existsByUserId(userId)).thenReturn(true);

        assertThrows(ProfileAlreadyExistsException.class, () -> service.create(userId, request));
        verify(users, never()).findById(any());
    }

    @Test
    void getOwnReturnsProfileWhenItExists() {
        UUID userId = UUID.randomUUID();
        var profile = new DeveloperProfile(user, "Bio", List.of("Java"), List.of("Spring"),
                ExperienceLevel.JUNIOR, null, null, null, NOW);
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile));

        var response = service.getOwn(userId);

        assertEquals("Bio", response.biography());
    }

    @Test
    void getOwnThrowsWhenProfileMissing() {
        UUID userId = UUID.randomUUID();
        when(profiles.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> service.getOwn(userId));
    }

    @Test
    void getPublicThrowsWhenProfileMissing() {
        UUID profileId = UUID.randomUUID();
        when(profiles.findWithUserById(profileId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> service.getPublic(profileId));
    }

    @Test
    void updateOwnAppliesChangesToExistingProfile() {
        UUID userId = UUID.randomUUID();
        var profile = new DeveloperProfile(user, "Old bio", List.of("Java"), List.of("Spring"),
                ExperienceLevel.JUNIOR, null, null, null, NOW);
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile));

        var response = service.updateOwn(userId, request);

        assertEquals("Backend developer", response.biography());
        assertEquals(ExperienceLevel.SEMI_SENIOR, response.experienceLevel());
    }

    @Test
    void updateByIdRejectsWhenCallerIsNotOwner() {
        UUID profileId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        var profile = new DeveloperProfile(user, "Bio", List.of("Java"), List.of("Spring"),
                ExperienceLevel.JUNIOR, null, null, null, NOW);
        when(profiles.findWithUserById(profileId)).thenReturn(Optional.of(profile));

        assertThrows(ProfileOwnershipException.class, () -> service.update(profileId, otherUserId, request));
    }

    @Test
    void updateByIdSucceedsForOwner() {
        UUID profileId = UUID.randomUUID();
        var profile = new DeveloperProfile(user, "Bio", List.of("Java"), List.of("Spring"),
                ExperienceLevel.JUNIOR, null, null, null, NOW);
        when(profiles.findWithUserById(profileId)).thenReturn(Optional.of(profile));

        var response = service.update(profileId, user.getId(), request);

        assertEquals("Backend developer", response.biography());
    }
}