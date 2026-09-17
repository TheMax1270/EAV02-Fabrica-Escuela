package com.eav02.backend.user.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.user.entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    @Query("select u from AppUser u where lower(u.email) = :identifier or lower(u.username) = :identifier")
    List<AppUser> findByLoginIdentifier(@Param("identifier") String identifier);

    @Query("select count(u) > 0 from AppUser u where lower(u.email) = :email")
    boolean existsByEmailNormalized(@Param("email") String email);

    @Query("select count(u) > 0 from AppUser u where lower(u.username) = :username")
    boolean existsByUsernameNormalized(@Param("username") String username);
}
