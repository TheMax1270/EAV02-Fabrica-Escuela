package com.eav02.backend.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eav02.backend.project.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("select p from Project p join fetch p.user where p.id = :id")
    Optional<Project> findWithUserById(@Param("id") UUID id);

    @Query("select p from Project p join fetch p.user where p.user.id = :userId order by p.createdAt desc")
    List<Project> findAllWithUserByUserId(@Param("userId") UUID userId);
}