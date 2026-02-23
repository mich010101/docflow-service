package com.app.docflow.infrastructure.persistence.history.entity;

import com.app.docflow.domain.document.DocumentHistoryAction;
import com.app.docflow.domain.document.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_history")
@Getter
@Setter
@NoArgsConstructor
public class DocumentHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private DocumentHistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private DocumentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private DocumentStatus toStatus;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
