package com.app.docflow.api.document.dto;

import java.util.List;

public record BatchOperationResponse(List<BatchItemResultResponse> results) {
}
