package com.hcl.zbankcard.dto.request;

import com.hcl.zbankcard.entity.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DocumentInfoRequest {

    @NotNull(message = "Document type is required")
    private DocumentType docType;

    @NotBlank(message = "Document number is required")
    @Size(min = 3, max = 100, message = "Document number must be between 3 and 100 characters")
    private String docNumber;

    private LocalDate expiryDate;
}
