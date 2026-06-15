package com.buddyduck.buddyduck.domain.room.dto;

public record RoomDetailResponse(
	Long id,
	String title,
	String viewerRole,
	String viewerJoinStatus,
	RoomPermissionsResponse permissions,
	long pendingRequestCount
) {
}
