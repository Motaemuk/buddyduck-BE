package com.buddyduck.buddyduck.domain.schedule.dto;

import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;

public record DraftRouteSegmentResponse(
	String fromClientId,
	String toClientId,
	RouteMode mode,
	Integer durationMinutes
) {

	public static DraftRouteSegmentResponse from(DraftRouteSegmentRequest request) {
		return new DraftRouteSegmentResponse(
			request.fromClientId(),
			request.toClientId(),
			request.mode(),
			request.durationMinutes()
		);
	}
}
