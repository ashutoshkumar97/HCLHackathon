package com.hcl.zbankcard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^\\d{4}$", message = "PIN must be exactly 4 digits")
    private String pin;
}
