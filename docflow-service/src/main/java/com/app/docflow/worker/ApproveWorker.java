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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApproveWorker {

    private final DocflowProperties properties;
    private final DocumentRepository documentRepository;
    private final DocumentCommandService documentCommandService;

    @Scheduled(fixedDelayString = "${docflow.workers.approveDelayMs:3000}")
    @LogExecutionTime("worker.approve")
    public void run() {
        if (!properties.getWorkers().isEnabled()) {
            return;
        }

        int batchSize = properties.getBatchSize();

        while (true) {
            List<UUID> ids = documentRepository.findIdsByStatus(DocumentStatus.SUBMITTED, batchSize);

            if (ids.isEmpty()) {
                return;
            }

            long started = System.currentTimeMillis();
            BatchOperationResult result = documentCommandService.approveBatch(ids);
            long elapsed = System.currentTimeMillis() - started;

            long remaining = documentRepository.countByStatus(DocumentStatus.SUBMITTED);

            log.info("approve worker batch: requested={}, success={}, elapsedMs={}, remainingStatusSUBMITTED={}",
                    ids.size(), result.successCount(), elapsed, remaining);

            if (ids.size() < batchSize) {
                return;
            }

        }
    }
}
