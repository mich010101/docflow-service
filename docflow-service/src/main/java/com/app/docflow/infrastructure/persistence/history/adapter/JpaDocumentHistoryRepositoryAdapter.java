package com.app.docflow.infrastructure.persistence.history.adapter;

import com.app.docflow.domain.document.DocumentHistoryEntry;
import com.app.docflow.domain.document.DocumentHistoryRepository;
import com.app.docflow.infrastructure.persistence.history.mapper.DocumentHistoryPersistenceMapper;
import com.app.docflow.infrastructure.persistence.history.repository.SpringDataDocumentHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaDocumentHistoryRepositoryAdapter implements DocumentHistoryRepository {

    private final SpringDataDocumentHistoryJpaRepository jpaRepository;
    private final DocumentHistoryPersistenceMapper mapper;

    @Override
    public void save(DocumentHistoryEntry entry) {
        jpaRepository.save(mapper.toEntity(entry));
    }

    @Override
    public List<DocumentHistoryEntry> findByDocumentId(UUID documentId) {
        return jpaRepository.findByDocumentIdOrderByCreatedAtAsc(documentId).stream().map(mapper::toDomain).toList();
    }
}
