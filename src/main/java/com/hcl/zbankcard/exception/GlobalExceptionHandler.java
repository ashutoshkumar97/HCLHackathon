package com.hcl.zbankcard.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hcl.zbankcard.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiResponse<>(404, "FAILED", ex.getMessage(), null));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {

		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiResponse<>(422, "FAILED", ex.getMessage(), null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiResponse<>(500, "FAILED", "Internal Server Error", null));
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
