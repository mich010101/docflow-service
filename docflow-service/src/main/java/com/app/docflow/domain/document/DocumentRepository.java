package com.app.docflow.domain.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findById(UUID id);

    List<Document> findAllByIds(List<UUID> ids);

    DocumentPage<Document> findByIds(List<UUID> ids, PageQuery pageQuery);

    DocumentPage<Document> search(DocumentSearchCriteria criteria, PageQuery pageQuery);

    List<UUID> findIdsByStatus(DocumentStatus status, int limit);

    long countByStatus(DocumentStatus status);

}
