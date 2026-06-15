package com.buddyduck.buddyduck.domain.room.dto;

import java.util.List;

public record MyRoomListResponse(
	List<MyRoomItemResponse> items,
	int page,
	int size,
	boolean hasNext
) {
}
