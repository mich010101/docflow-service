package com.app.docflow.api.document;

import com.app.docflow.api.common.dto.PageResponse;
import com.app.docflow.api.document.dto.CreateDocumentRequest;
import com.app.docflow.api.document.dto.DocumentResponse;
import com.app.docflow.api.error.GlobalExceptionHandler;
import com.app.docflow.application.document.command.DocumentCommandService;
import com.app.docflow.application.document.command.model.CreateDocumentCommand;
import com.app.docflow.application.document.concurrency.ConcurrencyApprovalCheckService;
import com.app.docflow.application.document.query.DocumentQueryService;
import com.app.docflow.domain.document.Document;
import com.app.docflow.domain.document.DocumentPage;
import com.app.docflow.domain.document.DocumentSearchCriteria;
import com.app.docflow.domain.document.DocumentStatus;
import com.app.docflow.domain.document.PageQuery;
import com.app.docflow.domain.document.SortDirection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(GlobalExceptionHandler.class)
class DocumentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentCommandService documentCommandService;

    @MockitoBean
    private DocumentQueryService documentQueryService;

    @MockitoBean
    private ConcurrencyApprovalCheckService concurrencyApprovalCheckService;

    @MockitoBean
    private DocumentApiMapper mapper;

    @Test
    void createReturnsCreatedResponse() throws Exception {
        UUID id = UUID.randomUUID();
        CreateDocumentCommand command = new CreateDocumentCommand("Doc", "alice", "body");
        Document domain = Document.restore(id, 1L, "DOC-00000001", "Doc", "alice", "body", DocumentStatus.DRAFT,
                Instant.parse("2026-02-23T10:00:00Z"), Instant.parse("2026-02-23T10:00:00Z"));
        DocumentResponse response = new DocumentResponse(id, "DOC-00000001", "Doc", "alice", "body", DocumentStatus.DRAFT,
                Instant.parse("2026-02-23T10:00:00Z"), Instant.parse("2026-02-23T10:00:00Z"));

        when(mapper.toCommand(any(CreateDocumentRequest.class))).thenReturn(command);
        when(documentCommandService.create(command)).thenReturn(domain);
        when(mapper.toResponse(domain)).thenReturn(response);

        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Doc\",\"author\":\"alice\",\"content\":\"body\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void batchGetUsesDefaultPagingAndSortingWhenFieldsAreNull() throws Exception {
        when(documentQueryService.getByIds(any(), any())).thenReturn(new DocumentPage<>(List.of(), 0, 0, 20, 0));
        when(mapper.toPageResponse(any())).thenReturn(new PageResponse<>(List.of(), 0, 0, 20, 0));

        mockMvc.perform(post("/api/v1/documents/_batch-get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<PageQuery> pageQueryCaptor = ArgumentCaptor.forClass(PageQuery.class);
        verify(documentQueryService).getByIds(any(), pageQueryCaptor.capture());
        PageQuery pageQuery = pageQueryCaptor.getValue();
        assertThat(pageQuery.page()).isEqualTo(0);
        assertThat(pageQuery.size()).isEqualTo(20);
        assertThat(pageQuery.sortBy()).isEqualTo("createdAt");
        assertThat(pageQuery.sortDirection()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void searchMapsRequestParamsToCriteriaAndPageQuery() throws Exception {
        when(documentQueryService.search(any(), any())).thenReturn(new DocumentPage<>(List.of(), 0, 0, 10, 0));
        when(mapper.toPageResponse(any())).thenReturn(new PageResponse<>(List.of(), 0, 0, 10, 0));

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("status", "SUBMITTED")
                        .param("author", "alice")
                        .param("createdFrom", "2026-02-20T00:00:00Z")
                        .param("createdTo", "2026-02-25T00:00:00Z")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sortBy", "title")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<DocumentSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(DocumentSearchCriteria.class);
        ArgumentCaptor<PageQuery> pageQueryCaptor = ArgumentCaptor.forClass(PageQuery.class);
        verify(documentQueryService).search(criteriaCaptor.capture(), pageQueryCaptor.capture());

        DocumentSearchCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.status()).isEqualTo(DocumentStatus.SUBMITTED);
        assertThat(criteria.author()).isEqualTo("alice");
        assertThat(criteria.createdFrom()).isEqualTo(Instant.parse("2026-02-20T00:00:00Z"));
        assertThat(criteria.createdTo()).isEqualTo(Instant.parse("2026-02-25T00:00:00Z"));

        PageQuery pageQuery = pageQueryCaptor.getValue();
        assertThat(pageQuery.page()).isEqualTo(1);
        assertThat(pageQuery.size()).isEqualTo(10);
        assertThat(pageQuery.sortBy()).isEqualTo("title");
        assertThat(pageQuery.sortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void createValidationReturnsUnifiedErrorFormat() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"\",\"content\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.author").exists());
    }
}
