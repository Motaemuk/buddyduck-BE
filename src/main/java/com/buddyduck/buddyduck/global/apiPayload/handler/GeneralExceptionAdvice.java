package com.buddyduck.buddyduck.global.apiPayload.handler;

import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.BaseErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

	@ExceptionHandler(ProjectException.class)
	public ResponseEntity<ApiResponse<Void>> handleProjectException(ProjectException exception) {
		BaseErrorCode errorCode = exception.getErrorCode();

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.onFailure(errorCode, null));
	}

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
		BaseErrorCode errorCode = GeneralErrorCode.BAD_REQUEST;

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.onFailure(errorCode, null));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<String>> handleMethodNotAllowed(Exception exception) {
		BaseErrorCode errorCode = GeneralErrorCode.METHOD_NOT_ALLOWED;

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.onFailure(errorCode, exception.getMessage()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<String>> handleNoResourceFoundException(Exception exception) {
		BaseErrorCode errorCode = GeneralErrorCode.NOT_FOUND;

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.onFailure(errorCode, exception.getMessage()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException() {
		BaseErrorCode errorCode = GeneralErrorCode.BAD_REQUEST;

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.onFailure(errorCode, null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
		BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR;
		log.error("Unhandled exception occurred", exception);

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.onFailure(errorCode, null));
	}
}
