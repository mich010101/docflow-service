package com.app.docflow.application.document.concurrency;

import com.app.docflow.application.document.command.DocumentCommandService;
import com.app.docflow.application.document.command.model.BatchOperationItemResult;
import com.app.docflow.application.document.concurrency.model.ConcurrencyApprovalCheckResult;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcurrencyApprovalCheckServiceTest {

    @Mock
    private DocumentCommandService documentCommandService;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private ConcurrencyApprovalCheckService service;

    @Test
    void runAggregatesResultsAndReturnsFinalStatus() {
        UUID id = UUID.randomUUID();
        AtomicInteger counter = new AtomicInteger();

        when(documentCommandService.approveOneForConcurrencyCheck(id)).thenAnswer(invocation -> {
            int index = counter.getAndIncrement();

            if (index == 0) {
                return BatchOperationItemResult.success(id, "Approved");
            }

            if (index == 1) {
                return BatchOperationItemResult.registryError(id, "Registry failed");
            }

            return BatchOperationItemResult.conflict(id, "Conflict");
        });

        when(documentRepository.findById(id)).thenReturn(Optional.of(approvedDocument(id)));

        ConcurrencyApprovalCheckResult result = service.run(id, 4, 5);

        assertThat(result.totalAttempts()).isEqualTo(5);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.conflictCount()).isEqualTo(3);
        assertThat(result.registryErrorCount()).isEqualTo(1);
        assertThat(result.notFoundCount()).isZero();
        assertThat(result.errorCount()).isZero();
        assertThat(result.finalStatus()).isEqualTo(DocumentStatus.APPROVED);
    }

    @Test
    void runRejectsInvalidThreads() {
        assertThatThrownBy(() -> service.run(UUID.randomUUID(), 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threads must be between");
    }

    @Test
    void runRejectsInvalidAttempts() {
        assertThatThrownBy(() -> service.run(UUID.randomUUID(), 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attempts must be between");
    }

    private Document approvedDocument(UUID id) {
        Instant now = Instant.parse("2026-02-23T10:00:00Z");
        return Document.restore(id, 1L, "DOC-00000001", "Title", "alice", "content", DocumentStatus.APPROVED, now, now);
    }

}
