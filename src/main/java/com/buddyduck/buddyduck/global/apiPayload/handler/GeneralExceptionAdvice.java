package com.buddyduck.buddyduck.global.apiPayload.handler;

import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.BaseErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeneralExceptionAdvice {

	@ExceptionHandler(ProjectException.class)
	public ResponseEntity<ApiResponse<Void>> handleProjectException(ProjectException exception) {
		BaseErrorCode errorCode = exception.getErrorCode();

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

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<String>> handleException(Exception exception) {
		BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR;

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.onFailure(errorCode, exception.getMessage()));
	}
}
