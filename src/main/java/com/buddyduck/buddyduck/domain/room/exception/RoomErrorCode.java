package com.buddyduck.buddyduck.domain.room.exception;

import com.buddyduck.buddyduck.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum RoomErrorCode implements BaseErrorCode {
	ROOM_FULL(HttpStatus.CONFLICT, "ROOM01", "정원이 가득 찼습니다."),
	DUPLICATE_JOIN(HttpStatus.CONFLICT, "JOIN01", "이미 신청했거나 참여 중인 방입니다."),
	INVALID_JOIN_STATUS(HttpStatus.CONFLICT, "JOIN02", "처리할 수 없는 입장 신청입니다.");

	private final HttpStatus status;
	private final String code;
	private final String message;

	RoomErrorCode(HttpStatus status, String code, String message) {
		this.status = status;
		this.code = code;
		this.message = message;
	}
}
