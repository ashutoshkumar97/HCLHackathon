package com.hcl.zbankcard.dto.request;

import com.hcl.zbankcard.entity.enums.EmploymentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EmploymentInfoRequest {

    @NotBlank(message = "Employer name is required")
    @Size(max = 200, message = "Employer name must not exceed 200 characters")
    private String employerName;

    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.01", message = "Salary must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Salary format is invalid")
    private BigDecimal salary;

    @NotBlank(message = "Designation is required")
    @Size(max = 150, message = "Designation must not exceed 150 characters")
    private String designation;
}
