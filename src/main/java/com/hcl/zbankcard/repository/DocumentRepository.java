package com.hcl.zbankcard.repository;

import com.hcl.zbankcard.entity.Document;
import com.hcl.zbankcard.entity.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCustomerIdAndDeletedFalse(UUID customerId);

    Optional<Document> findByCustomerIdAndDocTypeAndDeletedFalse(UUID customerId, DocumentType docType);

    Optional<Document> findByIdAndDeletedFalse(UUID id);

    boolean existsByDocNumber(String docNumber);
}
