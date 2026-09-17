package com.eav02.backend.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.auth.entity.AuthSession;

import jakarta.persistence.LockModeType;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSession s join fetch s.user where s.refreshTokenHash = :hash")
    Optional<AuthSession> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    @Query("select s from AuthSession s join fetch s.user where s.id = :id")
    Optional<AuthSession> findWithUserById(@Param("id") UUID id);
}
