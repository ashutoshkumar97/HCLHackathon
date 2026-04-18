package com.hcl.zbankcard.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private LocalDateTime timestamp;
    private int status;
    private String result; // SUCCESS / FAILED
    private String message;
    private T data;

    public ApiResponse(int status, String result, String message, T data) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.result = result;
        this.message = message;
        this.data = data;
    }
}