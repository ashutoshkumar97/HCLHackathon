package com.hcl.zbankcard.exception;
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}