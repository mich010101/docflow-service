package com.app.docflow.infrastructure.numbering;

import com.app.docflow.domain.document.ApprovalRegistryNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostgresSequenceApprovalRegistryNumberGenerator implements ApprovalRegistryNumberGenerator {

    private static final String NEXTVAL_SQL = "select nextval('approval_registry_number_seq')";
    private static final String APPROVAL_REGISTRY_NUMBER_PATTERN = "APR-%08d";
    private static final String ERROR_GENERATE_APPROVAL_REGISTRY_NUMBER = "Failed to generate approval registry number";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String nextRegistryNumber() {
        Long nextValue = jdbcTemplate.queryForObject(NEXTVAL_SQL, Long.class);

        if (nextValue == null) {
            throw new IllegalStateException(ERROR_GENERATE_APPROVAL_REGISTRY_NUMBER);
        }

        return APPROVAL_REGISTRY_NUMBER_PATTERN.formatted(nextValue);
    }

}
