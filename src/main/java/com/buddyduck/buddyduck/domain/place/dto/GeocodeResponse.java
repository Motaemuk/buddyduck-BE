package com.buddyduck.buddyduck.domain.place.dto;

import java.util.List;

public record GeocodeResponse(
	List<GeocodeItemResponse> items
) {
}
