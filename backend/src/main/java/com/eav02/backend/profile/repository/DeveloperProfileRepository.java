package com.eav02.backend.profile.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.profile.entity.DeveloperProfile;

public interface DeveloperProfileRepository extends JpaRepository<DeveloperProfile, UUID> {

    Optional<DeveloperProfile> findByUserId(UUID userId);

    @Query("select p from DeveloperProfile p join fetch p.user where p.id = :id")
    Optional<DeveloperProfile> findWithUserById(@Param("id") UUID id);

    boolean existsByUserId(UUID userId);
}
