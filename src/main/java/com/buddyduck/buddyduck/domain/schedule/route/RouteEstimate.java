package com.buddyduck.buddyduck.domain.schedule.route;

import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;

public record RouteEstimate(
	RouteMode mode,
	Integer distanceMeters,
	Integer durationMinutes,
	Integer taxiFareWon,
	Integer tollFareWon,
	String provider,
	Boolean manuallyAdjusted
) {

	public static RouteEstimate manual(RouteMode mode, Integer durationMinutes) {
		return new RouteEstimate(
			mode,
			null,
			durationMinutes,
			null,
			null,
			RouteEstimateProvider.MANUAL.name(),
			true
		);
	}

	public static RouteEstimate unresolvedPlace(RouteMode mode, Integer durationMinutes) {
		return new RouteEstimate(
			mode,
			null,
			durationMinutes,
			null,
			null,
			RouteEstimateProvider.UNRESOLVED_PLACE.name(),
			false
		);
	}
}
