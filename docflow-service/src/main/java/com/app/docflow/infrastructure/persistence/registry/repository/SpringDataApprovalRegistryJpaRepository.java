package com.app.docflow.infrastructure.persistence.registry.repository;

import com.app.docflow.infrastructure.persistence.registry.entity.ApprovalRegistryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataApprovalRegistryJpaRepository extends JpaRepository<ApprovalRegistryEntity, UUID> {

    Optional<ApprovalRegistryEntity> findByDocumentId(UUID documentId);

    long countByDocumentId(UUID documentId);

}
