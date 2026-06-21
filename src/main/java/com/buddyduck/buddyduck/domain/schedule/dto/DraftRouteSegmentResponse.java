package com.buddyduck.buddyduck.domain.schedule.dto;

import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimate;

public record DraftRouteSegmentResponse(
	String fromClientId,
	String toClientId,
	RouteMode mode,
	Integer distanceMeters,
	Integer durationMinutes,
	String provider,
	Boolean manuallyAdjusted
) {

	public static DraftRouteSegmentResponse from(DraftRouteSegmentRequest request, RouteEstimate estimate) {
		return new DraftRouteSegmentResponse(
			request.fromClientId(),
			request.toClientId(),
			request.mode(),
			estimate.distanceMeters(),
			estimate.durationMinutes(),
			estimate.provider(),
			estimate.manuallyAdjusted()
		);
	}
}
