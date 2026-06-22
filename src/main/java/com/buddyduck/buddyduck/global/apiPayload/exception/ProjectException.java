package com.buddyduck.buddyduck.global.apiPayload.exception;

import com.buddyduck.buddyduck.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class ProjectException extends RuntimeException {

	private final BaseErrorCode errorCode;
	private final Object result;

	public ProjectException(BaseErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.result = null;
	}

	public ProjectException(BaseErrorCode errorCode, Object result) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.result = result;
	}

	public ProjectException(BaseErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
		this.result = null;
	}
}
