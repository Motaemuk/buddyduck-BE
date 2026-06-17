package com.buddyduck.buddyduck.domain.room.dto;

import java.util.List;

public record RoomListResponse(
	List<RoomListItemResponse> items,
	int page,
	int size,
	boolean hasNext
) {
}
