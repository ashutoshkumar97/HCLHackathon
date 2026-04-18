package com.hcl.zbankcard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank
    private String cardNumber;

    @NotBlank
    private String pin;
}