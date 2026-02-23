package com.app.docflow.api.document;

import com.app.docflow.api.common.dto.PageResponse;
import com.app.docflow.api.document.dto.BatchGetDocumentsRequest;
import com.app.docflow.api.document.dto.BatchIdsRequest;
import com.app.docflow.api.document.dto.BatchOperationResponse;
import com.app.docflow.api.document.dto.ConcurrencyApprovalCheckRequest;
import com.app.docflow.api.document.dto.ConcurrencyApprovalCheckResponse;
import com.app.docflow.api.document.dto.CreateDocumentRequest;
import com.app.docflow.api.document.dto.DocumentDetailsResponse;
import com.app.docflow.api.document.dto.DocumentResponse;
import com.app.docflow.application.document.concurrency.ConcurrencyApprovalCheckService;
import com.app.docflow.application.document.command.DocumentCommandService;
import com.app.docflow.application.document.query.DocumentQueryService;
import com.app.docflow.domain.document.DocumentSearchCriteria;
import com.app.docflow.domain.document.DocumentStatus;
import com.app.docflow.domain.document.PageQuery;
import com.app.docflow.domain.document.SortDirection;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(DocumentController.BASE_PATH)
@RequiredArgsConstructor
public class DocumentController {

    static final String BASE_PATH = "/api/v1/documents";
    private static final String ID_PATH = "/{id}";
    private static final String BATCH_GET_PATH = "/_batch-get";
    private static final String SEARCH_PATH = "/search";
    private static final String SUBMIT_PATH = "/submit";
    private static final String APPROVE_PATH = "/approve";
    private static final String CONCURRENCY_APPROVE_CHECK_PATH = "/{id}/concurrency-approve-check";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIR = "DESC";

    private final DocumentCommandService documentCommandService;
    private final DocumentQueryService documentQueryService;
    private final ConcurrencyApprovalCheckService concurrencyApprovalCheckService;
    private final DocumentApiMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@Valid @RequestBody CreateDocumentRequest request) {
        return mapper.toResponse(documentCommandService.create(mapper.toCommand(request)));
    }

    @GetMapping(ID_PATH)
    public DocumentDetailsResponse getOne(@PathVariable UUID id) {
        return mapper.toDetailsResponse(documentQueryService.getDocument(id));
    }

    @PostMapping(BATCH_GET_PATH)
    public PageResponse<DocumentResponse> batchGet(@Valid @RequestBody BatchGetDocumentsRequest request) {
        return mapper.toPageResponse(documentQueryService.getByIds(
                request.ids(),
                toPageQuery(request.page(), request.size(), request.sortBy(), request.sortDir())
        ));
    }

    @GetMapping(SEARCH_PATH)
    public PageResponse<DocumentResponse> search(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIR) String sortDir
    ) {
        return mapper.toPageResponse(documentQueryService.search(
                new DocumentSearchCriteria(null, status, author, createdFrom, createdTo),
                toPageQuery(page, size, sortBy, sortDir)
        ));
    }

    @PostMapping(SUBMIT_PATH)
    public BatchOperationResponse submit(@Valid @RequestBody BatchIdsRequest request) {
        return mapper.toBatchResponse(documentCommandService.submitBatch(request.ids()));
    }

    @PostMapping(APPROVE_PATH)
    public BatchOperationResponse approve(@Valid @RequestBody BatchIdsRequest request) {
        return mapper.toBatchResponse(documentCommandService.approveBatch(request.ids()));
    }

    @PostMapping(CONCURRENCY_APPROVE_CHECK_PATH)
    public ConcurrencyApprovalCheckResponse concurrencyApproveCheck(
            @PathVariable UUID id,
            @Valid @RequestBody ConcurrencyApprovalCheckRequest request
    ) {
        return mapper.toConcurrencyResponse(
                concurrencyApprovalCheckService.run(id, request.threads(), request.attempts())
        );
    }

    private PageQuery toPageQuery(Integer page, Integer size, String sortBy, String sortDir) {
        return new PageQuery(
                page == null ? DEFAULT_PAGE : page,
                size == null ? DEFAULT_SIZE : size,
                (sortBy == null || sortBy.isBlank()) ? DEFAULT_SORT_BY : sortBy,
                parseSortDirection(sortDir)
        );
    }

    private SortDirection parseSortDirection(String value) {
        if (value == null || value.isBlank()) {
            return SortDirection.DESC;
        }

        return SortDirection.valueOf(value.trim().toUpperCase());
    }

}
