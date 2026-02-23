package com.app.docflow.infrastructure.persistence.document;

import com.app.docflow.domain.document.DocumentSearchCriteria;
import com.app.docflow.infrastructure.persistence.document.entity.DocumentEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class DocumentSpecifications {

    private static final String FIELD_ID = "id";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_CREATED_AT = "createdAt";

    private DocumentSpecifications() {
    }

    public static Specification<DocumentEntity> byCriteria(DocumentSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            if (criteria.ids() != null && !criteria.ids().isEmpty()) {
                predicates.add(root.get(FIELD_ID).in(criteria.ids()));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get(FIELD_STATUS), criteria.status()));
            }

            if (criteria.author() != null && !criteria.author().isBlank()) {
                predicates.add(cb.equal(root.get(FIELD_AUTHOR), criteria.author().trim()));
            }

            if (criteria.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(FIELD_CREATED_AT), criteria.createdFrom()));
            }

            if (criteria.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(FIELD_CREATED_AT), criteria.createdTo()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
