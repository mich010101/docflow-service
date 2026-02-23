package com.app.docflow.infrastructure.numbering;

import com.app.docflow.domain.document.DocumentNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresSequenceDocumentNumberGenerator implements DocumentNumberGenerator {

    private static final String NEXTVAL_SQL = "select nextval('document_number_seq')";
    private static final String DOCUMENT_NUMBER_PATTERN = "DOC-%08d";
    private static final String ERROR_GENERATE_DOCUMENT_NUMBER = "Failed to generate document number";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String nextDocumentNumber() {
        Long nextValue = jdbcTemplate.queryForObject(NEXTVAL_SQL, Long.class);

        if (nextValue == null) {
            throw new IllegalStateException(ERROR_GENERATE_DOCUMENT_NUMBER);
        }

        return DOCUMENT_NUMBER_PATTERN.formatted(nextValue);
    }

}
