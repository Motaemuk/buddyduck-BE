package com.buddyduck.buddyduck.global.apiPayload.exception;

import com.buddyduck.buddyduck.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class ProjectException extends RuntimeException {

	private final BaseErrorCode errorCode;

	public ProjectException(BaseErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
