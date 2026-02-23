package com.app.docflow.domain.document;

public record PageQuery(int page, int size, String sortBy, SortDirection sortDirection) {

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }

        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("size must be between 1 and 1000");
        }
    }

}
