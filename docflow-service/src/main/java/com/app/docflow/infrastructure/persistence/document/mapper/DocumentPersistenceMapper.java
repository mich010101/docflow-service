package com.app.docflow.infrastructure.persistence.document.mapper;

import com.app.docflow.domain.document.Document;
import com.app.docflow.infrastructure.persistence.document.entity.DocumentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentPersistenceMapper {

    DocumentEntity toEntity(Document document);

    default Document toDomain(DocumentEntity entity) {
        return Document.restore(
                entity.getId(),
                entity.getVersion(),
                entity.getNumber(),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getContent(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
