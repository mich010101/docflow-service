package com.app.docflow.application.document.query;

import com.app.docflow.application.document.query.model.DocumentDetailsView;
import com.app.docflow.domain.document.ApprovalRegistryRecord;
import com.app.docflow.domain.document.ApprovalRegistryRepository;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentHistoryEntry;
import com.app.docflow.domain.document.DocumentHistoryRepository;
import com.app.docflow.domain.document.DocumentNotFoundException;
import com.app.docflow.domain.document.DocumentPage;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentSearchCriteria;
import com.app.docflow.domain.document.DocumentStatus;
import com.app.docflow.domain.document.PageQuery;
import com.app.docflow.domain.document.SortDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentQueryServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentHistoryRepository documentHistoryRepository;

    @Mock
    private ApprovalRegistryRepository approvalRegistryRepository;

    @InjectMocks
    private DocumentQueryService service;

    @Test
    void getDocumentReturnsDocumentWithHistoryAndRegistry() {
        UUID id = UUID.randomUUID();
        Document document = document(id, DocumentStatus.APPROVED);
        List<DocumentHistoryEntry> history = List.of(DocumentHistoryEntry.created(id, Instant.parse("2026-02-23T10:00:00Z")));
        ApprovalRegistryRecord registryRecord = ApprovalRegistryRecord.restore(UUID.randomUUID(), id, "APR-00000001",
                Instant.parse("2026-02-23T10:05:00Z"));

        when(documentRepository.findById(id)).thenReturn(Optional.of(document));
        when(documentHistoryRepository.findByDocumentId(id)).thenReturn(history);
        when(approvalRegistryRepository.findByDocumentId(id)).thenReturn(Optional.of(registryRecord));

        DocumentDetailsView result = service.getDocument(id);

        assertThat(result.document()).isEqualTo(document);
        assertThat(result.history()).isEqualTo(history);
        assertThat(result.approvalRegistryRecord()).isEqualTo(registryRecord);
    }

    @Test
    void getDocumentThrowsWhenDocumentNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDocument(id))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getByIdsRejectsEmptyIds() {
        PageQuery pageQuery = new PageQuery(0, 20, "createdAt", SortDirection.DESC);

        assertThatThrownBy(() -> service.getByIds(List.of(), pageQuery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ids must not be empty");
    }

    @Test
    void searchDelegatesToRepository() {
        DocumentSearchCriteria criteria = new DocumentSearchCriteria(null, DocumentStatus.DRAFT, "alice", null, null);
        PageQuery pageQuery = new PageQuery(0, 10, "createdAt", SortDirection.ASC);
        DocumentPage<Document> expected = new DocumentPage<>(List.of(document(UUID.randomUUID(), DocumentStatus.DRAFT)), 1, 0, 10, 1);

        when(documentRepository.search(criteria, pageQuery)).thenReturn(expected);

        DocumentPage<Document> result = service.search(criteria, pageQuery);

        assertThat(result).isEqualTo(expected);
        verify(documentRepository).search(criteria, pageQuery);
    }

    private Document document(UUID id, DocumentStatus status) {
        Instant now = Instant.parse("2026-02-23T10:00:00Z");
        return Document.restore(id, 1L, "DOC-00000001", "Title", "alice", "content", status, now, now);
    }

}
