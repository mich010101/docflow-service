package com.app.docflow.api.common.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
}
