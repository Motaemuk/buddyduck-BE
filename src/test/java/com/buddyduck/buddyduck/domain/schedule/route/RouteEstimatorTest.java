package com.buddyduck.buddyduck.domain.schedule.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import com.buddyduck.buddyduck.domain.schedule.exception.ScheduleErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

class RouteEstimatorTest {

	private final KakaoMobilityRouteClient kakaoMobilityRouteClient = mock(KakaoMobilityRouteClient.class);
	private final FallbackRouteEstimator fallbackRouteEstimator = new FallbackRouteEstimator();
	private final RouteEstimator routeEstimator = new RouteEstimator(kakaoMobilityRouteClient, fallbackRouteEstimator);

	@Test
	void Kakao_Mobility가_비활성화되어_있으면_개발용_직선거리_추정값을_사용한다() {
		given(kakaoMobilityRouteClient.isEnabled()).willReturn(false);

		RouteEstimate estimate = routeEstimator.estimate(
			RouteMode.WALK,
			place("잠실역", "37.5130000", "127.1000000"),
			place("잠실 카페", "37.5150000", "127.1020000")
		);

		assertThat(estimate.provider()).isEqualTo(RouteEstimateProvider.FALLBACK_STRAIGHT_LINE.name());
	}

	@Test
	void Kakao_Mobility가_활성화되어_있는데_경로_계산에_실패하면_자동으로_직선거리_추정을_사용하지_않는다() {
		Place fromPlace = place("잠실역", "37.5130000", "127.1000000");
		Place toPlace = place("잠실 카페", "37.5150000", "127.1020000");
		given(kakaoMobilityRouteClient.isEnabled()).willReturn(true);
		given(kakaoMobilityRouteClient.estimate(RouteMode.WALK, fromPlace, toPlace))
			.willThrow(new RestClientException("Kakao Mobility failed"));

		assertThatThrownBy(() -> routeEstimator.estimate(RouteMode.WALK, fromPlace, toPlace))
			.isInstanceOfSatisfying(ProjectException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ScheduleErrorCode.ROUTE_ESTIMATION_FAILED)
			);
	}

	@Test
	void Kakao_Mobility가_활성화되어_있는데_응답이_비어도_자동으로_직선거리_추정을_사용하지_않는다() {
		Place fromPlace = place("잠실역", "37.5130000", "127.1000000");
		Place toPlace = place("잠실 카페", "37.5150000", "127.1020000");
		given(kakaoMobilityRouteClient.isEnabled()).willReturn(true);
		given(kakaoMobilityRouteClient.estimate(RouteMode.CAR_TAXI, fromPlace, toPlace))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> routeEstimator.estimate(RouteMode.CAR_TAXI, fromPlace, toPlace))
			.isInstanceOfSatisfying(ProjectException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ScheduleErrorCode.ROUTE_ESTIMATION_FAILED)
			);
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
