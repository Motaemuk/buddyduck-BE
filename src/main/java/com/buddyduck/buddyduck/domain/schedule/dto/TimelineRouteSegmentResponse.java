package com.buddyduck.buddyduck.domain.schedule.dto;

import com.buddyduck.buddyduck.domain.schedule.entity.RouteSegment;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;

public record TimelineRouteSegmentResponse(
	Long routeSegmentId,
	Long fromSlotId,
	Long toSlotId,
	RouteMode mode,
	Integer distanceMeters,
	Integer durationMinutes,
	Integer taxiFareWon,
	Integer tollFareWon,
	String provider,
	Boolean manuallyAdjusted
) {

	public static TimelineRouteSegmentResponse from(RouteSegment routeSegment) {
		return new TimelineRouteSegmentResponse(
			routeSegment.getId(),
			routeSegment.getFromSlot().getId(),
			routeSegment.getToSlot().getId(),
			routeSegment.getMode(),
			routeSegment.getDistanceMeters(),
			routeSegment.getDurationMinutes(),
			routeSegment.getTaxiFareWon(),
			routeSegment.getTollFareWon(),
			routeSegment.getProvider(),
			routeSegment.getManuallyAdjusted()
		);
	}
}
