package com.app.docflow.worker;

import com.app.docflow.aop.LogExecutionTime;
import com.app.docflow.application.document.command.DocumentCommandService;
import com.app.docflow.application.document.command.model.BatchOperationResult;
import com.app.docflow.config.DocflowProperties;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitWorker {

    private final DocflowProperties properties;
    private final DocumentRepository documentRepository;
    private final DocumentCommandService documentCommandService;

    @Scheduled(fixedDelayString = "${docflow.workers.submitDelayMs:3000}")
    @LogExecutionTime("worker.submit")
    public void run() {
        if (!properties.getWorkers().isEnabled()) {
            return;
        }

        processDraftDocuments();
    }

    private void processDraftDocuments() {
        DocumentStatus status = DocumentStatus.DRAFT;
        int batchSize = properties.getBatchSize();

        while (true) {
            List<java.util.UUID> ids = documentRepository.findIdsByStatus(status, batchSize);

            if (ids.isEmpty()) {
                return;
            }

            long started = System.currentTimeMillis();
            BatchOperationResult result = documentCommandService.submitBatch(ids);
            long elapsed = System.currentTimeMillis() - started;

            long remaining = documentRepository.countByStatus(status);

            log.info("submit worker batch: requested={}, success={}, elapsedMs={}, remainingStatus={}={}",
                    ids.size(), result.successCount(), elapsed, status, remaining);

            if (ids.size() < batchSize) {
                return;
            }
        }
    }
}
