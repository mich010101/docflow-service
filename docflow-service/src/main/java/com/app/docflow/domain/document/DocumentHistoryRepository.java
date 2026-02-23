package com.app.docflow.domain.document;

import java.util.List;
import java.util.UUID;

public interface DocumentHistoryRepository {
    void save(DocumentHistoryEntry entry);

    List<DocumentHistoryEntry> findByDocumentId(UUID documentId);
}
