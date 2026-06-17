package com.buddyduck.buddyduck.domain.schedule.dto;

public record MapBoundsResponse(
	MapPointResponse southWest,
	MapPointResponse northEast
) {
}
