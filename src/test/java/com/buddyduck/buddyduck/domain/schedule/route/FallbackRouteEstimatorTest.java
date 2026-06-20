package com.buddyduck.buddyduck.domain.schedule.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FallbackRouteEstimatorTest {

	private final FallbackRouteEstimator estimator = new FallbackRouteEstimator();

	@Test
	void WALK는_직선거리_보정값으로_거리와_시간을_추정한다() {
		RouteEstimate estimate = estimator.estimate(
			RouteMode.WALK,
			place("잠실역", "37.5130000", "127.1000000"),
			place("잠실 카페", "37.5150000", "127.1020000")
		);

		assertThat(estimate.provider()).isEqualTo(RouteEstimateProvider.FALLBACK_STRAIGHT_LINE.name());
		assertThat(estimate.mode()).isEqualTo(RouteMode.WALK);
		assertThat(estimate.distanceMeters()).isBetween(300, 450);
		assertThat(estimate.durationMinutes()).isPositive();
		assertThat(estimate.taxiFareWon()).isNull();
		assertThat(estimate.tollFareWon()).isNull();
	}

	@Test
	void CAR_TAXI는_직선거리_보정값으로_거리와_시간을_추정하고_요금은_비운다() {
		RouteEstimate estimate = estimator.estimate(
			RouteMode.CAR_TAXI,
			place("잠실역", "37.5130000", "127.1000000"),
			place("KSPO Dome", "37.5190000", "127.1270000")
		);

		assertThat(estimate.provider()).isEqualTo(RouteEstimateProvider.FALLBACK_STRAIGHT_LINE.name());
		assertThat(estimate.mode()).isEqualTo(RouteMode.CAR_TAXI);
		assertThat(estimate.distanceMeters()).isGreaterThan(2_000);
		assertThat(estimate.durationMinutes()).isPositive();
		assertThat(estimate.taxiFareWon()).isNull();
		assertThat(estimate.tollFareWon()).isNull();
	}

	private Place place(String name, String lat, String lng) {
		return Place.create(
			PlaceSource.KAKAO_ADDRESS,
			name,
			name,
			"서울 송파구",
			new BigDecimal(lat),
			new BigDecimal(lng)
		);
	}
}
