package com.buddyduck.buddyduck.domain.room.dto;

public record MyRoomItemResponse(
	Long roomId,
	String title,
	String viewerRole,
	String viewerJoinStatus,
	String roomStatus,
	String concertTitle,
	String concertStartAt,
	long daysUntilConcert,
	String venueName,
	String meetingAt,
	String meetingPlaceName,
	String meetingPlaceAddress,
	long memberCount,
	Integer maxMembers,
	long pendingJoinRequestCount
) {
}
