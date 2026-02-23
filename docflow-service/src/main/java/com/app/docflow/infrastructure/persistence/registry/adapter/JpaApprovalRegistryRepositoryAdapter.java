package com.app.docflow.infrastructure.persistence.registry.adapter;

import com.app.docflow.domain.document.ApprovalRegistryRepository;
import com.app.docflow.domain.document.ApprovalRegistryRecord;
import com.app.docflow.domain.document.ApprovalRegistryWriteException;
import com.app.docflow.infrastructure.persistence.registry.mapper.ApprovalRegistryPersistenceMapper;
import com.app.docflow.infrastructure.persistence.registry.repository.SpringDataApprovalRegistryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaApprovalRegistryRepositoryAdapter implements ApprovalRegistryRepository {

    private static final String APPROVAL_REGISTRY_WRITE_FAILED_MESSAGE = "Approval registry write failed";

    private final SpringDataApprovalRegistryJpaRepository jpaRepository;
    private final ApprovalRegistryPersistenceMapper mapper;

    @Override
    public void save(ApprovalRegistryRecord record) {
        try {
            jpaRepository.save(mapper.toEntity(record));
        } catch (DataIntegrityViolationException e) {
            throw new ApprovalRegistryWriteException(APPROVAL_REGISTRY_WRITE_FAILED_MESSAGE, e);
        }
    }

    @Override
    public Optional<ApprovalRegistryRecord> findByDocumentId(UUID documentId) {
        return jpaRepository.findByDocumentId(documentId).map(mapper::toDomain);
    }

    @Override
    public long countByDocumentId(UUID documentId) {
        return jpaRepository.countByDocumentId(documentId);
    }
}
