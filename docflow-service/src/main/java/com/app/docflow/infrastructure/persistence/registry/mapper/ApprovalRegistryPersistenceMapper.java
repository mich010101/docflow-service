package com.app.docflow.infrastructure.persistence.registry.mapper;

import com.app.docflow.domain.document.ApprovalRegistryRecord;
import com.app.docflow.infrastructure.persistence.registry.entity.ApprovalRegistryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApprovalRegistryPersistenceMapper {

    ApprovalRegistryEntity toEntity(ApprovalRegistryRecord record);

    default ApprovalRegistryRecord toDomain(ApprovalRegistryEntity entity) {
        return ApprovalRegistryRecord.restore(
                entity.getId(),
                entity.getDocumentId(),
                entity.getRegistryNumber(),
                entity.getApprovedAt()
        );
    }

}
