package com.app.docflow.api.document;

import com.app.docflow.api.common.dto.PageResponse;
import com.app.docflow.api.document.dto.ApprovalRegistryResponse;
import com.app.docflow.api.document.dto.BatchItemResultResponse;
import com.app.docflow.api.document.dto.BatchOperationResponse;
import com.app.docflow.api.document.dto.ConcurrencyApprovalCheckResponse;
import com.app.docflow.api.document.dto.CreateDocumentRequest;
import com.app.docflow.api.document.dto.DocumentDetailsResponse;
import com.app.docflow.api.document.dto.DocumentHistoryResponse;
import com.app.docflow.api.document.dto.DocumentResponse;
import com.app.docflow.application.document.command.model.BatchOperationResult;
import com.app.docflow.application.document.concurrency.model.ConcurrencyApprovalCheckResult;
import com.app.docflow.application.document.command.model.CreateDocumentCommand;
import com.app.docflow.application.document.query.model.DocumentDetailsView;
import com.app.docflow.domain.document.ApprovalRegistryRecord;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentHistoryEntry;
import com.app.docflow.domain.document.DocumentPage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentApiMapper {

    CreateDocumentCommand toCommand(CreateDocumentRequest request);

    DocumentResponse toResponse(Document document);

    DocumentHistoryResponse toHistoryResponse(DocumentHistoryEntry entry);

    default ApprovalRegistryResponse toRegistryResponse(ApprovalRegistryRecord record) {
        if (record == null) {
            return null;
        }

        return new ApprovalRegistryResponse(record.getId(), record.getRegistryNumber(), record.getApprovedAt());
    }

    default DocumentDetailsResponse toDetailsResponse(DocumentDetailsView view) {
        return new DocumentDetailsResponse(
                toResponse(view.document()),
                view.history().stream().map(this::toHistoryResponse).toList(),
                toRegistryResponse(view.approvalRegistryRecord())
        );
    }

    default PageResponse<DocumentResponse> toPageResponse(DocumentPage<Document> page) {
        List<DocumentResponse> items = page.items().stream().map(this::toResponse).toList();
        return new PageResponse<>(items, page.totalElements(), page.page(), page.size(), page.totalPages());
    }

    default BatchOperationResponse toBatchResponse(BatchOperationResult result) {
        return new BatchOperationResponse(result.results().stream()
                .map(item -> new BatchItemResultResponse(item.documentId(), item.status(), item.message()))
                .toList());
    }

    default ConcurrencyApprovalCheckResponse toConcurrencyResponse(ConcurrencyApprovalCheckResult result) {
        return new ConcurrencyApprovalCheckResponse(
                result.totalAttempts(),
                result.successCount(),
                result.conflictCount(),
                result.notFoundCount(),
                result.registryErrorCount(),
                result.errorCount(),
                result.finalStatus()
        );
    }

}
