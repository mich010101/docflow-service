package com.app.docflow.infrastructure.persistence.document.repository;

import com.app.docflow.domain.document.DocumentStatus;
import com.app.docflow.infrastructure.persistence.document.entity.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataDocumentJpaRepository extends JpaRepository<DocumentEntity, UUID>, JpaSpecificationExecutor<DocumentEntity> {

    Page<DocumentEntity> findByIdIn(List<UUID> ids, Pageable pageable);

    long countByStatus(DocumentStatus status);

    @Query("select d.id from DocumentEntity d where d.status = :status order by d.createdAt asc")
    List<UUID> findIdsByStatus(@Param("status") DocumentStatus status, Pageable pageable);

}
