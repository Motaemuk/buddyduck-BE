package com.buddyduck.buddyduck.domain.room.dto;

import java.util.List;

public record JoinRequestListResponse(
	List<JoinRequestListItemResponse> items,
	int page,
	int size,
	boolean hasNext
) {
}
