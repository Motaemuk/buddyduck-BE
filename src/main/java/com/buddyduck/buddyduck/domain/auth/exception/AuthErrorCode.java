package com.buddyduck.buddyduck.domain.auth.exception;

import com.buddyduck.buddyduck.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements BaseErrorCode {
	REQUIRED_PROFILE_INFO(
		HttpStatus.BAD_REQUEST,
		"AUTH_REQUIRED_PROFILE_INFO",
		"성별/연령대 동의가 필요합니다."
	),
	INVALID_REDIRECT_URI(
		HttpStatus.BAD_REQUEST,
		"AUTH_INVALID_REDIRECT_URI",
		"허용되지 않은 redirectUri입니다."
	),
	EXTERNAL_ERROR(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"AUTH_EXTERNAL_ERROR",
		"Kakao 로그인 처리에 실패했습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;

	AuthErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
