package com.buddyduck.buddyduck.domain.room.dto;

public record MyRoomItemResponse(
	Long roomId,
	String title,
	String viewerRole,
	String viewerJoinStatus
) {
}
