package com.buddyduck.buddyduck.domain.place.dto;

import java.util.List;

public record PlaceSearchResponse(
	List<PlaceSearchItemResponse> items
) {
}
