package com.buddyduck.buddyduck.domain.schedule.route;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoMobilityRouteClient {

	private static final String AUTHORIZATION_PREFIX = "KakaoAK ";
	private static final int WALKING_METERS_PER_MINUTE = 60;

	private final RestClient restClient;
	private final KakaoMobilityProperties properties;

	public KakaoMobilityRouteClient(RestClient.Builder restClientBuilder, KakaoMobilityProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	public boolean isEnabled() {
		return properties.enabled();
	}

	public Optional<RouteEstimate> estimate(RouteMode mode, Place fromPlace, Place toPlace) {
		if (!isEnabled()) {
			return Optional.empty();
		}
		if (mode == RouteMode.WALK) {
			return estimateWalkingByDrivingDistance(fromPlace, toPlace);
		}
		return estimateDriving(fromPlace, toPlace);
	}

	private Optional<RouteEstimate> estimateWalkingByDrivingDistance(Place fromPlace, Place toPlace) {
		return requestDrivingSummary(fromPlace, toPlace)
			.map(summary -> new RouteEstimate(
				RouteMode.WALK,
				summary.distance(),
				toWalkingMinutes(summary.distance()),
				null,
				null,
				RouteEstimateProvider.DRIVING_DISTANCE_WALK_ESTIMATE.name(),
				false
			));
	}

	private Optional<RouteEstimate> estimateDriving(Place fromPlace, Place toPlace) {
		return requestDrivingSummary(fromPlace, toPlace)
			.map(summary -> new RouteEstimate(
				RouteMode.CAR_TAXI,
				summary.distance(),
				toMinutes(summary.duration()),
				summary.fare() == null ? null : summary.fare().taxi(),
				summary.fare() == null ? null : summary.fare().toll(),
				RouteEstimateProvider.KAKAO_DRIVING.name(),
				false
			));
	}

	private Optional<KakaoDrivingSummary> requestDrivingSummary(Place fromPlace, Place toPlace) {
		KakaoDrivingDirectionsResponse response = restClient.get()
			.uri(
				properties.getDrivingDirectionsUri()
					+ "?origin={origin}&destination={destination}&priority=RECOMMEND&summary=true",
				coordinate(fromPlace),
				coordinate(toPlace)
			)
			.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + properties.getRestApiKey())
			.retrieve()
			.body(KakaoDrivingDirectionsResponse.class);

		return drivingSummary(response);
	}

	private Optional<KakaoDrivingSummary> drivingSummary(KakaoDrivingDirectionsResponse response) {
		if (response == null || response.routes() == null) {
			return Optional.empty();
		}
		return response.routes().stream()
			.filter(route -> route.resultCode() == null || route.resultCode() == 0)
			.map(KakaoDrivingRoute::summary)
			.filter(summary -> summary != null && summary.distance() != null && summary.duration() != null)
			.findFirst();
	}

	private String coordinate(Place place) {
		return place.getLng().toPlainString() + "," + place.getLat().toPlainString();
	}

	private int toMinutes(Integer seconds) {
		if (seconds == null || seconds == 0) {
			return 0;
		}
		return Math.max(1, (seconds + 59) / 60);
	}

	private int toWalkingMinutes(Integer meters) {
		if (meters == null || meters == 0) {
			return 0;
		}
		return Math.max(1, (meters + WALKING_METERS_PER_MINUTE - 1) / WALKING_METERS_PER_MINUTE);
	}

	private record KakaoDrivingDirectionsResponse(
		List<KakaoDrivingRoute> routes
	) {
	}

	private record KakaoDrivingRoute(
		@JsonProperty("result_code")
		Integer resultCode,
		KakaoDrivingSummary summary
	) {
	}

	private record KakaoDrivingSummary(
		Integer distance,
		Integer duration,
		KakaoDrivingFare fare
	) {
	}

	private record KakaoDrivingFare(
		Integer taxi,
		Integer toll
	) {
	}
}
