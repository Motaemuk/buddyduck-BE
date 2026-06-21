package com.buddyduck.buddyduck.domain.room.dto;

public record RoomDetailConcertResponse(
	Long id,
	String title,
	String startAt,
	String venueName
) {
}
