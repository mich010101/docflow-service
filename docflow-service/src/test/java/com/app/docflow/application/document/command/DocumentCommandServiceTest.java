package com.app.docflow.application.document.command;

import com.app.docflow.application.document.command.model.BatchOperationItemResult;
import com.app.docflow.application.document.command.model.BatchOperationItemStatus;
import com.app.docflow.application.document.command.model.BatchOperationResult;
import com.app.docflow.application.document.command.model.CreateDocumentCommand;
import com.app.docflow.application.support.RequiresNewTransactionRunner;
import com.app.docflow.domain.document.ApprovalRegistryNumberGenerator;
import com.app.docflow.domain.document.ApprovalRegistryRepository;
import com.app.docflow.domain.document.ApprovalRegistryWriteException;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentHistoryRepository;
import com.app.docflow.domain.document.DocumentNumberGenerator;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentCommandServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentHistoryRepository documentHistoryRepository;

    @Mock
    private ApprovalRegistryRepository approvalRegistryRepository;

    @Mock
    private DocumentNumberGenerator documentNumberGenerator;

    @Mock
    private ApprovalRegistryNumberGenerator approvalRegistryNumberGenerator;

    @Mock
    private RequiresNewTransactionRunner requiresNewTransactionRunner;

    private DocumentCommandService service;
    private final List<TransactionStatus> transactionStatuses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-02-23T10:00:00Z"), ZoneOffset.UTC);

        service = new DocumentCommandService(
                documentRepository,
                documentHistoryRepository,
                approvalRegistryRepository,
                documentNumberGenerator,
                approvalRegistryNumberGenerator,
                fixedClock,
                requiresNewTransactionRunner
        );

        transactionStatuses.clear();

        lenient().doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<TransactionStatus, Object> callback = (Function<TransactionStatus, Object>) invocation.getArgument(0);
            TransactionStatus txStatus = mock(TransactionStatus.class);
            transactionStatuses.add(txStatus);
            return callback.apply(txStatus);
        }).when(requiresNewTransactionRunner).run(any());
    }

    @Test
    void createCreatesDraftDocumentAndHistoryEntry() {
        CreateDocumentCommand command = new CreateDocumentCommand("  Title  ", " alice ", " body ");
        when(documentNumberGenerator.nextDocumentNumber()).thenReturn("DOC-00000001");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document result = service.create(command);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(result.getNumber()).isEqualTo("DOC-00000001");
        assertThat(result.getTitle()).isEqualTo("Title");
        assertThat(result.getAuthor()).isEqualTo("alice");
        verify(documentHistoryRepository).save(any());
    }

    @Test
    void submitBatchPrefetchesUniqueIdsAndReturnsConflictForDuplicates() {
        UUID existingId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        List<UUID> ids = List.of(existingId, existingId, missingId);
        Document draftDocument = document(existingId, DocumentStatus.DRAFT);

        when(documentRepository.findAllByIds(List.of(existingId, missingId))).thenReturn(List.of(draftDocument));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchOperationResult result = service.submitBatch(ids);

        assertThat(result.results()).hasSize(3);
        assertThat(result.results().get(0).status()).isEqualTo(BatchOperationItemStatus.SUCCESS);
        assertThat(result.results().get(1).status()).isEqualTo(BatchOperationItemStatus.CONFLICT);
        assertThat(result.results().get(1).message()).contains("Duplicate");
        assertThat(result.results().get(2).status()).isEqualTo(BatchOperationItemStatus.NOT_FOUND);
        verify(documentRepository).findAllByIds(List.of(existingId, missingId));
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void approveBatchReturnsRegistryErrorAndMarksItemTransactionForRollback() {
        UUID id = UUID.randomUUID();
        Document submittedDocument = document(id, DocumentStatus.SUBMITTED);

        when(documentRepository.findAllByIds(List.of(id))).thenReturn(List.of(submittedDocument));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalRegistryNumberGenerator.nextRegistryNumber()).thenReturn("APR-00000001");
        doThrow(new ApprovalRegistryWriteException("Approval registry write failed", new RuntimeException("forced")))
                .when(approvalRegistryRepository).save(any());

        BatchOperationResult result = service.approveBatch(List.of(id));
        BatchOperationItemResult item = result.results().get(0);

        assertThat(item.status()).isEqualTo(BatchOperationItemStatus.REGISTRY_ERROR);
        verify(transactionStatuses.get(0)).setRollbackOnly();
    }

    @Test
    void approveOneForConcurrencyCheckLoadsDocumentById() {
        UUID id = UUID.randomUUID();
        Document submittedDocument = document(id, DocumentStatus.SUBMITTED);

        when(documentRepository.findById(id)).thenReturn(java.util.Optional.of(submittedDocument));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalRegistryNumberGenerator.nextRegistryNumber()).thenReturn("APR-00000001");

        BatchOperationItemResult result = service.approveOneForConcurrencyCheck(id);

        assertThat(result.status()).isEqualTo(BatchOperationItemStatus.SUCCESS);
        verify(documentRepository, never()).findAllByIds(any());
        verify(documentRepository).findById(id);
    }

    @Test
    void submitBatchRejectsNullIdsInsideList() {
        List<UUID> ids = new ArrayList<>();

        ids.add(UUID.randomUUID());
        ids.add(null);

        assertThatThrownBy(() -> service.submitBatch(ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ids must not contain null values");
    }

    @Test
    void approveBatchRejectsListLargerThanMaxBatchSize() {
        List<UUID> ids = new ArrayList<>();

        for (int i = 0; i < 1001; i++) {
            ids.add(UUID.randomUUID());
        }

        assertThatThrownBy(() -> service.approveBatch(ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ids size must be between 1 and 1000");
    }

    private Document document(UUID id, DocumentStatus status) {
        Instant now = Instant.parse("2026-02-23T10:00:00Z");
        return Document.restore(id, 1L, "DOC-00000001", "Title", "alice", "content", status, now, now);
    }

}
