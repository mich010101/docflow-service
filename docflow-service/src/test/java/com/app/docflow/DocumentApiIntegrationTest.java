package com.app.docflow;

import com.app.docflow.domain.document.ApprovalRegistryWriteException;
import com.app.docflow.domain.document.DocumentStatus;
import com.app.docflow.infrastructure.persistence.document.repository.SpringDataDocumentJpaRepository;
import com.app.docflow.infrastructure.persistence.registry.adapter.JpaApprovalRegistryRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "docflow.workers.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class DocumentApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("docflow_test")
            .withUsername("docflow")
            .withPassword("docflow");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.yaml");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SpringDataDocumentJpaRepository documentJpaRepository;

    @MockitoSpyBean
    JpaApprovalRegistryRepositoryAdapter approvalRegistryRepositoryAdapter;

    @BeforeEach
    void setUp() {
        reset(approvalRegistryRepositoryAdapter);
        documentJpaRepository.deleteAll();
    }

    @Test
    void happyPathSingleDocumentCreateSubmitApproveAndGetWithHistory() throws Exception {
        String created = createDocument("Doc 1", "alice");
        String id = jsonField(created, "id");

        mockMvc.perform(post("/api/v1/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")));

        mockMvc.perform(post("/api/v1/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")));

        mockMvc.perform(get("/api/v1/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.id", is(id)))
                .andExpect(jsonPath("$.document.status", is("APPROVED")))
                .andExpect(jsonPath("$.history", hasSize(3)))
                .andExpect(jsonPath("$.approvalRegistry.registryNumber").exists());
    }

    @Test
    void createReturnsDraftWithGeneratedNumber() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Create API\",\"author\":\"alice\",\"content\":\"body\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.number", startsWith("DOC-")))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void batchSubmitReturnsSuccessForMultipleDraftDocuments() throws Exception {
        String id1 = jsonField(createDocument("Doc 1", "alice"), "id");
        String id2 = jsonField(createDocument("Doc 2", "bob"), "id");

        mockMvc.perform(post("/api/v1/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id1 + "\",\"" + id2 + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[1].status", is("SUCCESS")));
    }

    @Test
    void batchApproveReturnsPartialResults() throws Exception {
        String submittedId = jsonField(createDocument("Doc submitted", "alice"), "id");
        String draftId = jsonField(createDocument("Doc draft", "alice"), "id");
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + submittedId + "\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + submittedId + "\",\"" + draftId + "\",\"" + missingId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[1].status", is("CONFLICT")))
                .andExpect(jsonPath("$.results[2].status", is("NOT_FOUND")));
    }

    @Test
    void approveRollbackWhenRegistryWriteFails() throws Exception {
        String id = jsonField(createDocument("Doc rollback", "alice"), "id");
        mockMvc.perform(post("/api/v1/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")));

        doThrow(new ApprovalRegistryWriteException("Approval registry write failed", new RuntimeException("forced registry failure")))
                .when(approvalRegistryRepositoryAdapter)
                .save(ArgumentMatchers.argThat(record -> record.getDocumentId().toString().equals(id)));

        mockMvc.perform(post("/api/v1/documents/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status", is("REGISTRY_ERROR")));

        mockMvc.perform(get("/api/v1/documents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.status", is(DocumentStatus.SUBMITTED.name())))
                .andExpect(jsonPath("$.approvalRegistry", nullValue()));
    }

    @Test
    void batchGetReturnsPagedAndSortedDocumentsByIds() throws Exception {
        String idB = jsonField(createDocument("B title", "alice"), "id");
        String idA = jsonField(createDocument("A title", "alice"), "id");
        String idC = jsonField(createDocument("C title", "alice"), "id");

        mockMvc.perform(post("/api/v1/documents/_batch-get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ids":["%s","%s","%s"],
                                  "page":0,
                                  "size":2,
                                  "sortBy":"title",
                                  "sortDir":"ASC"
                                }
                                """.formatted(idB, idA, idC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title", is("A title")))
                .andExpect(jsonPath("$.items[1].title", is("B title")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void searchFiltersByStatusAuthorAndCreatedPeriod() throws Exception {
        String aliceDraftId = jsonField(createDocument("Search draft", "alice"), "id");
        String aliceSubmittedId = jsonField(createDocument("Search submitted", "alice"), "id");
        createDocument("Search other", "bob");

        mockMvc.perform(post("/api/v1/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + aliceSubmittedId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")));

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("status", "SUBMITTED")
                        .param("author", "alice")
                        .param("createdFrom", "2020-01-01T00:00:00Z")
                        .param("createdTo", "2030-01-01T00:00:00Z")
                        .param("sortBy", "title")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id", is(aliceSubmittedId)))
                .andExpect(jsonPath("$.items[0].status", is("SUBMITTED")));

        mockMvc.perform(get("/api/v1/documents/search")
                        .param("author", "alice")
                        .param("createdFrom", "2030-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void concurrencyApproveCheckReturnsSingleSuccessAndApprovedFinalStatus() throws Exception {
        String id = jsonField(createDocument("Concurrency doc", "alice"), "id");
        mockMvc.perform(post("/api/v1/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")));

        mockMvc.perform(post("/api/v1/documents/{id}/concurrency-approve-check", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threads\":8,\"attempts\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts", is(20)))
                .andExpect(jsonPath("$.successCount", is(1)))
                .andExpect(jsonPath("$.finalStatus", is("APPROVED")));
    }

    @Test
    void validationErrorReturnsUnifiedFormat() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"\",\"content\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.author").exists());
    }

    @Test
    void getOneMissingDocumentReturnsNotFoundWithUnifiedError() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DOCUMENT_NOT_FOUND")))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void batchSubmitDuplicateIdsReturnsConflictForDuplicateOccurrence() throws Exception {
        String id = jsonField(createDocument("Duplicate submit", "alice"), "id");

        mockMvc.perform(post("/api/v1/documents/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[\"" + id + "\",\"" + id + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(2)))
                .andExpect(jsonPath("$.results[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.results[1].status", is("CONFLICT")));
    }

    private String createDocument(String title, String author) throws Exception {
        return mockMvc.perform(post("/api/v1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"author\":\"" + author + "\",\"content\":\"test\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String jsonField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key);

        if (start < 0) {
            throw new IllegalStateException("Field not found: " + fieldName + " in " + json);
        }

        int valueStart = start + key.length();
        int valueEnd = json.indexOf('"', valueStart);

        return json.substring(valueStart, valueEnd);
    }

}
