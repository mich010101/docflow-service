package com.app.docflow.infrastructure.persistence.history.repository;

import com.app.docflow.infrastructure.persistence.history.entity.DocumentHistoryEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDocumentHistoryJpaRepository extends JpaRepository<DocumentHistoryEntity, UUID> {

    List<DocumentHistoryEntity> findByDocumentIdOrderByCreatedAtAsc(UUID documentId);

}
