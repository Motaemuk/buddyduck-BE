package com.buddyduck.buddyduck.domain.schedule.exception;

import com.buddyduck.buddyduck.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ScheduleErrorCode implements BaseErrorCode {
	ROUTE_ESTIMATION_FAILED(
		HttpStatus.BAD_GATEWAY,
		"SCHEDULE_ROUTE_ESTIMATION_FAILED",
		"경로 계산에 실패했습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String message;

	ScheduleErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
