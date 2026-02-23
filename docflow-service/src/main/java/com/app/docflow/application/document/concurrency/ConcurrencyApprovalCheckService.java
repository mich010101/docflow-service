package com.app.docflow.application.document.concurrency;

import com.app.docflow.aop.LogExecutionTime;
import com.app.docflow.application.document.command.model.BatchOperationItemResult;
import com.app.docflow.application.document.command.model.BatchOperationItemStatus;
import com.app.docflow.application.document.command.DocumentCommandService;
import com.app.docflow.application.document.concurrency.model.ConcurrencyApprovalCheckResult;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class ConcurrencyApprovalCheckService {

    private static final int MIN_THREADS = 1;
    private static final int MAX_THREADS = 256;
    private static final int MIN_ATTEMPTS = 1;
    private static final int MAX_ATTEMPTS = 10_000;
    private static final String MESSAGE_UNEXPECTED_ERROR = "Unexpected error";

    private final DocumentCommandService documentCommandService;
    private final DocumentRepository documentRepository;

    @LogExecutionTime("document.concurrencyApproveCheck")
    public ConcurrencyApprovalCheckResult run(UUID documentId, int threads, int attempts) {
        if (threads < MIN_THREADS || threads > MAX_THREADS) {
            throw new IllegalArgumentException("threads must be between %d and %d".formatted(MIN_THREADS, MAX_THREADS));
        }

        if (attempts < MIN_ATTEMPTS || attempts > MAX_ATTEMPTS) {
            throw new IllegalArgumentException("attempts must be between %d and %d".formatted(MIN_ATTEMPTS, MAX_ATTEMPTS));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {

            List<Callable<BatchOperationItemResult>> tasks = new ArrayList<>(attempts);

            for (int i = 0; i < attempts; i++) {
                tasks.add(() -> documentCommandService.approveOneForConcurrencyCheck(documentId));
            }

            List<Future<BatchOperationItemResult>> futures = executor.invokeAll(tasks);
            List<BatchOperationItemResult> results = new ArrayList<>(attempts);

            for (Future<BatchOperationItemResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException e) {
                    results.add(BatchOperationItemResult.error(
                            documentId,
                            e.getCause() == null ? MESSAGE_UNEXPECTED_ERROR : e.getCause().getMessage()
                    ));
                }
            }

            long successCount = countByStatus(results, BatchOperationItemStatus.SUCCESS);
            long conflictCount = countByStatus(results, BatchOperationItemStatus.CONFLICT);
            long notFoundCount = countByStatus(results, BatchOperationItemStatus.NOT_FOUND);
            long registryErrorCount = countByStatus(results, BatchOperationItemStatus.REGISTRY_ERROR);
            long errorCount = countByStatus(results, BatchOperationItemStatus.ERROR);
            DocumentStatus finalStatus = documentRepository.findById(documentId).map(Document::getStatus).orElse(null);

            return new ConcurrencyApprovalCheckResult(
                    attempts,
                    successCount,
                    conflictCount,
                    notFoundCount,
                    registryErrorCount,
                    errorCount,
                    finalStatus
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency check interrupted", e);
        } finally {
            executor.shutdown();
        }
    }

    private long countByStatus(List<BatchOperationItemResult> results, BatchOperationItemStatus status) {
        return results.stream()
                .filter(result -> result.status() == status)
                .count();
    }

}
