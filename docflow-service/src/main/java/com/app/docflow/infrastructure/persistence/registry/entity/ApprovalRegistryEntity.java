package com.app.docflow.infrastructure.persistence.registry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_registry")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalRegistryEntity {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Column(name = "registry_number", nullable = false, unique = true, length = 64)
    private String registryNumber;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

}
