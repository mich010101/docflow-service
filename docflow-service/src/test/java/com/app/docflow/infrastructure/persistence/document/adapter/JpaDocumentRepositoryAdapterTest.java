package com.app.docflow.infrastructure.persistence.document.adapter;

import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentConflictException;
import com.app.docflow.domain.document.DocumentStatus;
import com.app.docflow.infrastructure.persistence.document.entity.DocumentEntity;
import com.app.docflow.infrastructure.persistence.document.mapper.DocumentPersistenceMapper;
import com.app.docflow.infrastructure.persistence.document.repository.SpringDataDocumentJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaDocumentRepositoryAdapterTest {

    @Mock
    private SpringDataDocumentJpaRepository jpaRepository;

    @Mock
    private DocumentPersistenceMapper mapper;

    @InjectMocks
    private JpaDocumentRepositoryAdapter adapter;

    @Test
    void saveWrapsOptimisticLockExceptionIntoDomainConflict() {
        Document document = document(UUID.randomUUID(), DocumentStatus.DRAFT);

        when(mapper.toEntity(document)).thenReturn(new DocumentEntity());
        when(jpaRepository.save(any())).thenThrow(new ObjectOptimisticLockingFailureException(DocumentEntity.class, document.getId()));

        assertThatThrownBy(() -> adapter.save(document))
                .isInstanceOf(DocumentConflictException.class)
                .hasMessageContaining("Concurrent modification conflict");
    }

    @Test
    void findAllByIdsMapsEntitiesToDomain() {
        UUID id = UUID.randomUUID();
        DocumentEntity entity = new DocumentEntity();
        entity.setId(id);
        Document domain = document(id, DocumentStatus.DRAFT);

        when(jpaRepository.findAllById(List.of(id))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Document> result = adapter.findAllByIds(List.of(id));

        assertThat(result).containsExactly(domain);
    }

    private Document document(UUID id, DocumentStatus status) {
        Instant now = Instant.parse("2026-02-23T10:00:00Z");
        return Document.restore(id, 1L, "DOC-00000001", "Title", "alice", "content", status, now, now);
    }

}
