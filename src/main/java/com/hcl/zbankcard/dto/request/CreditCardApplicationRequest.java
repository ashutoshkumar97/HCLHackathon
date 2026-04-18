package com.hcl.zbankcard.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreditCardApplicationRequest {

    @NotNull(message = "Customer information is required")
    @Valid
    private CustomerInfoRequest customerInfo;

    @NotNull(message = "Employment information is required")
    @Valid
    private EmploymentInfoRequest employmentInfo;

    @NotNull(message = "Identity document information is required")
    @Valid
    private DocumentInfoRequest documentInfo;
}
