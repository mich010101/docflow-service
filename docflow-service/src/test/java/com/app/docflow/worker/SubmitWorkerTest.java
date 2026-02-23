package com.app.docflow.worker;

import com.app.docflow.application.document.command.DocumentCommandService;
import com.app.docflow.application.document.command.model.BatchOperationItemResult;
import com.app.docflow.application.document.command.model.BatchOperationResult;
import com.app.docflow.config.DocflowProperties;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitWorkerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentCommandService documentCommandService;

    @InjectMocks
    private SubmitWorker worker;

    @Test
    void runDoesNothingWhenWorkersDisabled() {
        DocflowProperties properties = new DocflowProperties();
        properties.getWorkers().setEnabled(false);
        worker = new SubmitWorker(properties, documentRepository, documentCommandService);

        worker.run();

        verify(documentRepository, never()).findIdsByStatus(DocumentStatus.DRAFT, properties.getBatchSize());
        verify(documentCommandService, never()).submitBatch(anyList());
    }

    @Test
    void runProcessesDraftDocumentsInBatchesUntilLastPartialBatch() {
        DocflowProperties properties = new DocflowProperties();
        properties.setBatchSize(2);
        worker = new SubmitWorker(properties, documentRepository, documentCommandService);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        when(documentRepository.findIdsByStatus(DocumentStatus.DRAFT, 2))
                .thenReturn(List.of(id1, id2))
                .thenReturn(List.of(id3));
        when(documentRepository.countByStatus(DocumentStatus.DRAFT)).thenReturn(1L, 0L);
        when(documentCommandService.submitBatch(List.of(id1, id2))).thenReturn(successResult(id1, id2));
        when(documentCommandService.submitBatch(List.of(id3))).thenReturn(successResult(id3));

        worker.run();

        verify(documentCommandService).submitBatch(List.of(id1, id2));
        verify(documentCommandService).submitBatch(List.of(id3));
    }

    private BatchOperationResult successResult(UUID... ids) {
        return new BatchOperationResult(Arrays.stream(ids)
                .map(id -> BatchOperationItemResult.success(id, "ok"))
                .toList());
    }

}
