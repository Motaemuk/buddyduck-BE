package com.buddyduck.buddyduck.global.apiPayload.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GeneralSuccessCode implements BaseSuccessCode {
	OK(HttpStatus.OK, "COMMON200", "요청에 성공했습니다."),
	CREATED(HttpStatus.CREATED, "COMMON201", "생성에 성공했습니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	GeneralSuccessCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
