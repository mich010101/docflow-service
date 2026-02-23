package com.app.docflow.infrastructure.persistence.registry.adapter;

import com.app.docflow.domain.document.ApprovalRegistryRecord;
import com.app.docflow.domain.document.ApprovalRegistryWriteException;
import com.app.docflow.infrastructure.persistence.registry.entity.ApprovalRegistryEntity;
import com.app.docflow.infrastructure.persistence.registry.mapper.ApprovalRegistryPersistenceMapper;
import com.app.docflow.infrastructure.persistence.registry.repository.SpringDataApprovalRegistryJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaApprovalRegistryRepositoryAdapterTest {

    @Mock
    private SpringDataApprovalRegistryJpaRepository jpaRepository;

    @Mock
    private ApprovalRegistryPersistenceMapper mapper;

    @InjectMocks
    private JpaApprovalRegistryRepositoryAdapter adapter;

    @Test
    void saveWrapsDataIntegrityViolationException() {
        ApprovalRegistryRecord record = ApprovalRegistryRecord.restore(UUID.randomUUID(), UUID.randomUUID(), "APR-00000001",
                Instant.parse("2026-02-23T10:00:00Z"));
        when(mapper.toEntity(record)).thenReturn(new ApprovalRegistryEntity());
        when(jpaRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> adapter.save(record))
                .isInstanceOf(ApprovalRegistryWriteException.class)
                .hasMessageContaining("Approval registry write failed");
    }

    @Test
    void findByDocumentIdMapsOptionalEntityToDomain() {
        UUID documentId = UUID.randomUUID();
        ApprovalRegistryEntity entity = new ApprovalRegistryEntity();
        entity.setId(UUID.randomUUID());
        entity.setDocumentId(documentId);
        entity.setRegistryNumber("APR-00000001");
        entity.setApprovedAt(Instant.parse("2026-02-23T10:00:00Z"));
        ApprovalRegistryRecord record = ApprovalRegistryRecord.restore(entity.getId(), documentId, entity.getRegistryNumber(), entity.getApprovedAt());

        when(jpaRepository.findByDocumentId(documentId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(record);

        Optional<ApprovalRegistryRecord> result = adapter.findByDocumentId(documentId);

        assertThat(result).contains(record);
    }

}
