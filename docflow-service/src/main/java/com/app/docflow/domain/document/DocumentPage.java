package com.app.docflow.domain.document;

import java.util.List;

public record DocumentPage<T>(List<T> items, long totalElements, int page, int size, int totalPages) {
}
