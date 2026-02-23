package com.app.docflow.api.document.dto;

import java.util.List;

public record DocumentDetailsResponse(
        DocumentResponse document,
        List<DocumentHistoryResponse> history,
        ApprovalRegistryResponse approvalRegistry
) {
}
