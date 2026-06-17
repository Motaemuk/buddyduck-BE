package com.buddyduck.buddyduck.domain.room.dto;

public record JoinRequestApproveResponse(
	Long requestId,
	String status,
	Long memberId
) {
}
