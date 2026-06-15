package com.buddyduck.buddyduck.domain.room.dto;

public record RoomPermissionsResponse(
	boolean canRequestJoin,
	boolean canApproveJoinRequest,
	boolean canViewOpenChat,
	boolean canOpenTimeline,
	boolean canEditRoom
) {
}
