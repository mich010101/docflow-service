package com.app.docflow.application.document.command;

import com.app.docflow.aop.LogExecutionTime;
import com.app.docflow.application.document.command.model.BatchOperationItemResult;
import com.app.docflow.application.document.command.model.BatchOperationResult;
import com.app.docflow.application.document.command.model.CreateDocumentCommand;
import com.app.docflow.application.support.RequiresNewTransactionRunner;
import com.app.docflow.domain.document.ApprovalRegistryRecord;
import com.app.docflow.domain.document.ApprovalRegistryRepository;
import com.app.docflow.domain.document.ApprovalRegistryNumberGenerator;
import com.app.docflow.domain.document.ApprovalRegistryWriteException;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentConflictException;
import com.app.docflow.domain.document.DocumentHistoryEntry;
import com.app.docflow.domain.document.DocumentHistoryRepository;
import com.app.docflow.domain.document.DocumentNotFoundException;
import com.app.docflow.domain.document.DocumentNumberGenerator;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.InvalidDocumentStateTransitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentCommandService implements CreateDocumentUseCase {

    private static final int MAX_BATCH_SIZE = 1000;
    private static final String MESSAGE_SUBMITTED = "Submitted";
    private static final String MESSAGE_APPROVED = "Approved";
    private static final String MESSAGE_UNEXPECTED_ERROR = "Unexpected error";
    private static final String MESSAGE_DUPLICATE_ID_IN_REQUEST = "Duplicate document id in request";

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository documentHistoryRepository;
    private final ApprovalRegistryRepository approvalRegistryRepository;
    private final DocumentNumberGenerator documentNumberGenerator;
    private final ApprovalRegistryNumberGenerator approvalRegistryNumberGenerator;
    private final Clock clock;
    private final RequiresNewTransactionRunner requiresNewTransactionRunner;

    @Override
    @Transactional
    @LogExecutionTime("document.create")
    public Document create(CreateDocumentCommand command) {
        Objects.requireNonNull(command, "command is required");

        Instant now = Instant.now(clock);

        Document document = Document.createDraft(
                UUID.randomUUID(),
                documentNumberGenerator.nextDocumentNumber(),
                command.title(),
                command.author(),
                command.content(),
                now
        );

        Document saved = documentRepository.save(document);

        documentHistoryRepository.save(DocumentHistoryEntry.created(saved.getId(), now));

        log.info("Document created: id={}, number={}", saved.getId(), saved.getNumber());

        return saved;
    }

    @LogExecutionTime("document.submitBatch")
    public BatchOperationResult submitBatch(List<UUID> ids) {
        validateBatchIds(ids);
        Map<UUID, Document> prefetchedDocumentsById = prefetchDocumentsById(ids);
        return new BatchOperationResult(processBatch(ids, prefetchedDocumentsById, this::submitOne));
    }

    @LogExecutionTime("document.approveBatch")
    public BatchOperationResult approveBatch(List<UUID> ids) {
        validateBatchIds(ids);
        Map<UUID, Document> prefetchedDocumentsById = prefetchDocumentsById(ids);
        return new BatchOperationResult(processBatch(ids, prefetchedDocumentsById, this::approveOne));
    }

    public BatchOperationItemResult approveOneForConcurrencyCheck(UUID id) {
        return requiresNewTransactionRunner.run(status -> approveOne(id, status));
    }

    private List<BatchOperationItemResult> processBatch(
            List<UUID> ids,
            Map<UUID, Document> prefetchedDocumentsById,
            BatchItemProcessor processor
    ) {
        List<BatchOperationItemResult> results = new ArrayList<>(ids.size());
        Set<UUID> seenIds = new HashSet<>(ids.size());

        for (UUID id : ids) {
            if (!seenIds.add(id)) {
                results.add(BatchOperationItemResult.conflict(id, MESSAGE_DUPLICATE_ID_IN_REQUEST));
                continue;
            }
            Document prefetchedDocument = prefetchedDocumentsById.get(id);
            results.add(requiresNewTransactionRunner.run(status -> processor.process(id, prefetchedDocument, status)));
        }

        return results;
    }

    private Map<UUID, Document> prefetchDocumentsById(List<UUID> ids) {
        List<UUID> uniqueIds = new ArrayList<>(new LinkedHashSet<>(ids));
        List<Document> prefetchedDocuments = documentRepository.findAllByIds(uniqueIds);

        Map<UUID, Document> documentsById = new HashMap<>(prefetchedDocuments.size());

        for (Document document : prefetchedDocuments) {
            documentsById.put(document.getId(), document);
        }

        return documentsById;
    }

    private BatchOperationItemResult submitOne(UUID id, Document prefetchedDocument, TransactionStatus txStatus) {
        try {
            Document document = requirePrefetchedDocument(id, prefetchedDocument);
            Instant now = Instant.now(clock);
            Document updated = documentRepository.save(document.submit(now));
            documentHistoryRepository.save(DocumentHistoryEntry.submitted(updated.getId(), now));
            return BatchOperationItemResult.success(id, MESSAGE_SUBMITTED);
        } catch (DocumentNotFoundException e) {
            return BatchOperationItemResult.notFound(id, e.getMessage());
        } catch (InvalidDocumentStateTransitionException | DocumentConflictException e) {
            return BatchOperationItemResult.conflict(id, e.getMessage());
        } catch (RuntimeException e) {
            txStatus.setRollbackOnly();
            log.error("Submit failed for document {}", id, e);
            return BatchOperationItemResult.error(id, MESSAGE_UNEXPECTED_ERROR);
        }
    }

    private BatchOperationItemResult approveOne(UUID id, Document prefetchedDocument, TransactionStatus txStatus) {
        try {
            Document document = requirePrefetchedDocument(id, prefetchedDocument);
            return approveAndRegister(id, document);
        } catch (DocumentNotFoundException e) {
            return BatchOperationItemResult.notFound(id, e.getMessage());
        } catch (InvalidDocumentStateTransitionException | DocumentConflictException e) {
            return BatchOperationItemResult.conflict(id, e.getMessage());
        } catch (ApprovalRegistryWriteException e) {
            txStatus.setRollbackOnly();
            return BatchOperationItemResult.registryError(id, e.getMessage());
        } catch (RuntimeException e) {
            txStatus.setRollbackOnly();
            log.error("Approve failed for document {}", id, e);
            return BatchOperationItemResult.error(id, MESSAGE_UNEXPECTED_ERROR);
        }
    }

    private BatchOperationItemResult approveOne(UUID id, TransactionStatus txStatus) {
        try {
            Document document = loadDocument(id);
            return approveAndRegister(id, document);
        } catch (DocumentNotFoundException e) {
            return BatchOperationItemResult.notFound(id, e.getMessage());
        } catch (InvalidDocumentStateTransitionException | DocumentConflictException e) {
            return BatchOperationItemResult.conflict(id, e.getMessage());
        } catch (ApprovalRegistryWriteException e) {
            txStatus.setRollbackOnly();
            return BatchOperationItemResult.registryError(id, e.getMessage());
        } catch (RuntimeException e) {
            txStatus.setRollbackOnly();
            log.error("Approve failed for document {}", id, e);
            return BatchOperationItemResult.error(id, MESSAGE_UNEXPECTED_ERROR);
        }
    }

    private BatchOperationItemResult approveAndRegister(UUID id, Document document) {
        Instant now = Instant.now(clock);
        Document approved = documentRepository.save(document.approve(now));
        String registryNumber = approvalRegistryNumberGenerator.nextRegistryNumber();
        approvalRegistryRepository.save(ApprovalRegistryRecord.create(approved.getId(), registryNumber, now));
        documentHistoryRepository.save(DocumentHistoryEntry.approved(approved.getId(), now));
        return BatchOperationItemResult.success(id, MESSAGE_APPROVED);
    }

    private Document loadDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    private Document requirePrefetchedDocument(UUID id, Document prefetchedDocument) {
        if (prefetchedDocument == null) {
            throw new DocumentNotFoundException(id);
        }

        return prefetchedDocument;
    }

    private void validateBatchIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }

        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("ids size must be between 1 and 1000");
        }

        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("ids must not contain null values");
        }
    }

    @FunctionalInterface
    private interface BatchItemProcessor {

        BatchOperationItemResult process(UUID id, Document prefetchedDocument, TransactionStatus txStatus);

    }

}
