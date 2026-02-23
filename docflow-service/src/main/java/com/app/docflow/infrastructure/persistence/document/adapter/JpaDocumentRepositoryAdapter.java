package com.app.docflow.infrastructure.persistence.document.adapter;

import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentConflictException;
import com.app.docflow.domain.document.DocumentPage;
import com.app.docflow.domain.document.DocumentRepository;
import com.app.docflow.domain.document.DocumentSearchCriteria;
import com.app.docflow.domain.document.DocumentStatus;
import com.app.docflow.domain.document.PageQuery;
import com.app.docflow.domain.document.SortDirection;
import com.app.docflow.infrastructure.persistence.document.DocumentSpecifications;
import com.app.docflow.infrastructure.persistence.document.entity.DocumentEntity;
import com.app.docflow.infrastructure.persistence.document.mapper.DocumentPersistenceMapper;
import com.app.docflow.infrastructure.persistence.document.repository.SpringDataDocumentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JpaDocumentRepositoryAdapter implements DocumentRepository {

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String CONCURRENT_MODIFICATION_CONFLICT_MESSAGE = "Concurrent modification conflict";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(DEFAULT_SORT_FIELD, "updatedAt", "number", "title", "author", "status");

    private final SpringDataDocumentJpaRepository jpaRepository;
    private final DocumentPersistenceMapper mapper;

    @Override
    public Document save(Document document) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(document)));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new DocumentConflictException(CONCURRENT_MODIFICATION_CONFLICT_MESSAGE, e);
        }
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Document> findAllByIds(List<UUID> ids) {
        return jpaRepository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public DocumentPage<Document> findByIds(List<UUID> ids, PageQuery pageQuery) {
        Page<DocumentEntity> page = jpaRepository.findByIdIn(ids, toPageable(pageQuery));

        return new DocumentPage<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages()
        );
    }

    @Override
    public DocumentPage<Document> search(DocumentSearchCriteria criteria, PageQuery pageQuery) {
        Page<DocumentEntity> page = jpaRepository.findAll(
                DocumentSpecifications.byCriteria(criteria),
                toPageable(pageQuery)
        );

        return new DocumentPage<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages()
        );
    }

    @Override
    public List<UUID> findIdsByStatus(DocumentStatus status, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, DEFAULT_SORT_FIELD));
        return jpaRepository.findIdsByStatus(status, pageable);
    }

    @Override
    public long countByStatus(DocumentStatus status) {
        return jpaRepository.countByStatus(status);
    }

    private Pageable toPageable(PageQuery pageQuery) {
        String sortBy = ALLOWED_SORT_FIELDS.contains(pageQuery.sortBy()) ? pageQuery.sortBy() : DEFAULT_SORT_FIELD;
        Sort.Direction direction = pageQuery.sortDirection() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(direction, sortBy));
    }

}
