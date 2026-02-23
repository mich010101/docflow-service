package com.app.docflow.application.document.command.model;

import java.util.List;

public record BatchOperationResult(List<BatchOperationItemResult> results) {

    public int successCount() {
        return (int) results.stream()
                .filter(result -> result.status() == BatchOperationItemStatus.SUCCESS)
                .count();
    }

}
