package com.app.docflow.infrastructure.persistence.history.mapper;

import com.app.docflow.domain.document.DocumentHistoryEntry;
import com.app.docflow.infrastructure.persistence.history.entity.DocumentHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentHistoryPersistenceMapper {

    DocumentHistoryEntity toEntity(DocumentHistoryEntry entry);

    default DocumentHistoryEntry toDomain(DocumentHistoryEntity entity) {
        return DocumentHistoryEntry.restore(
                entity.getId(),
                entity.getDocumentId(),
                entity.getAction(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }

}
