package com.app.docflow.application.document.query.model;

import com.app.docflow.domain.document.ApprovalRegistryRecord;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentHistoryEntry;

import java.util.List;

public record DocumentDetailsView(
        Document document,
        List<DocumentHistoryEntry> history,
        ApprovalRegistryRecord approvalRegistryRecord
) {
}
