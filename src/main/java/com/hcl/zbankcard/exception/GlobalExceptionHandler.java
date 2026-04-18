package com.hcl.zbankcard.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(ApiError.builder().status(HttpStatus.UNPROCESSABLE_ENTITY.value())
						.error("Business Rule Violation").message(ex.getMessage())
						.path(request.getRequestURI()).build());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationErrors(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		List<ApiError.FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> ApiError.FieldValidationError.builder().field(fe.getField())
						.rejectedValue(fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : null)
						.message(fe.getDefaultMessage()).build())
				.toList();

		ApiError error = ApiError.builder().status(HttpStatus.BAD_REQUEST.value()).error("Validation Failed")
				.message("One or more fields are invalid").path(request.getRequestURI()).fieldErrors(fieldErrors)
				.build();

		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(DuplicateCustomerException.class)
	public ResponseEntity<ApiError> handleDuplicateCustomer(DuplicateCustomerException ex, HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.builder().status(HttpStatus.CONFLICT.value())
				.error("Conflict").message(ex.getMessage()).path(request.getRequestURI()).build());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
		log.warn("Invalid credentials at {}: {}", request.getRequestURI(), ex.getMessage());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiError.builder().status(HttpStatus.UNAUTHORIZED.value())
						.error("Unauthorized").message(ex.getMessage())
						.path(request.getRequestURI()).build());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.builder().status(HttpStatus.NOT_FOUND.value())
				.error("Not Found").message(ex.getMessage()).path(request.getRequestURI()).build());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneral(Exception ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.builder().status(HttpStatus.INTERNAL_SERVER_ERROR.value()).error("Internal Server Error")
						.message("An unexpected error occurred. Please try again later.").path(request.getRequestURI())
						.build());
	}
}
