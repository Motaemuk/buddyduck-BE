package com.buddyduck.buddyduck.domain.schedule.route;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import org.springframework.stereotype.Component;

@Component
public class FallbackRouteEstimator {

	private static final double EARTH_RADIUS_METERS = 6_371_000.0;
	private static final double WALK_DISTANCE_FACTOR = 1.25;
	private static final double TAXI_DISTANCE_FACTOR = 1.35;
	private static final double WALK_SPEED_METERS_PER_MINUTE = 60.0;
	private static final double TAXI_SPEED_METERS_PER_MINUTE = 22_000.0 / 60.0;

	public RouteEstimate estimate(RouteMode mode, Place fromPlace, Place toPlace) {
		int distanceMeters = estimateDistanceMeters(mode, fromPlace, toPlace);
		int durationMinutes = estimateDurationMinutes(mode, distanceMeters);
		return new RouteEstimate(
			mode,
			distanceMeters,
			durationMinutes,
			null,
			null,
			RouteEstimateProvider.FALLBACK_STRAIGHT_LINE.name(),
			false
		);
	}

	private int estimateDistanceMeters(RouteMode mode, Place fromPlace, Place toPlace) {
		double straightDistance = straightDistanceMeters(fromPlace, toPlace);
		double factor = mode == RouteMode.WALK ? WALK_DISTANCE_FACTOR : TAXI_DISTANCE_FACTOR;
		return (int) Math.ceil(straightDistance * factor);
	}

	private int estimateDurationMinutes(RouteMode mode, int distanceMeters) {
		if (distanceMeters == 0) {
			return 0;
		}

		double speed = mode == RouteMode.WALK ? WALK_SPEED_METERS_PER_MINUTE : TAXI_SPEED_METERS_PER_MINUTE;
		return Math.max(1, (int) Math.ceil(distanceMeters / speed));
	}

	private double straightDistanceMeters(Place fromPlace, Place toPlace) {
		double fromLat = Math.toRadians(fromPlace.getLat().doubleValue());
		double toLat = Math.toRadians(toPlace.getLat().doubleValue());
		double latDiff = toLat - fromLat;
		double lngDiff = Math.toRadians(toPlace.getLng().doubleValue() - fromPlace.getLng().doubleValue());

		double a = Math.pow(Math.sin(latDiff / 2), 2)
			+ Math.cos(fromLat) * Math.cos(toLat) * Math.pow(Math.sin(lngDiff / 2), 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METERS * c;
	}
}
