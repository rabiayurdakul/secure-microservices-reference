package com.reference.catalogdataservice.repository;

import com.reference.catalogdataservice.entity.IdempotencyKeyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, String> {
}
