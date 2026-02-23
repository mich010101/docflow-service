package com.app.docflow.application.document.query;

import com.app.docflow.application.document.query.model.DocumentDetailsView;
import com.app.docflow.domain.document.ApprovalRegistryRepository;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentHistoryRepository;
import com.app.docflow.domain.document.DocumentNotFoundException;
import com.app.docflow.domain.document.DocumentPage;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentSearchCriteria;
import com.app.docflow.domain.document.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentQueryService {

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository documentHistoryRepository;
    private final ApprovalRegistryRepository approvalRegistryRepository;

    public DocumentDetailsView getDocument(UUID id) {
        Document document = documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
        return new DocumentDetailsView(
                document,
                documentHistoryRepository.findByDocumentId(id),
                approvalRegistryRepository.findByDocumentId(id).orElse(null)
        );
    }

    public DocumentPage<Document> getByIds(List<UUID> ids, PageQuery pageQuery) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }

        return documentRepository.findByIds(ids, pageQuery);
    }

    public DocumentPage<Document> search(DocumentSearchCriteria criteria, PageQuery pageQuery) {
        return documentRepository.search(criteria, pageQuery);
    }

}
