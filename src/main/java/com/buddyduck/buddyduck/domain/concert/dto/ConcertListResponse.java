package com.buddyduck.buddyduck.domain.concert.dto;

import java.util.List;

public record ConcertListResponse(
	List<ConcertSummaryResponse> items,
	int page,
	int size,
	boolean hasNext
) {
}
