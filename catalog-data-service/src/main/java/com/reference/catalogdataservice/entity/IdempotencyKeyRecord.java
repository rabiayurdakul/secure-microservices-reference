package com.reference.catalogdataservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyKeyRecord {

    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
