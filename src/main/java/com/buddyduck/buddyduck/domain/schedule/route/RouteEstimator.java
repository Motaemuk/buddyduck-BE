package com.buddyduck.buddyduck.domain.schedule.route;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import com.buddyduck.buddyduck.domain.schedule.exception.ScheduleErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class RouteEstimator {

	private final KakaoMobilityRouteClient kakaoMobilityRouteClient;
	private final FallbackRouteEstimator fallbackRouteEstimator;

	public RouteEstimate estimate(RouteMode mode, Place fromPlace, Place toPlace) {
		if (kakaoMobilityRouteClient.isEnabled()) {
			try {
				return kakaoMobilityRouteClient.estimate(mode, fromPlace, toPlace)
					.orElseThrow(() -> new ProjectException(ScheduleErrorCode.ROUTE_ESTIMATION_FAILED));
			} catch (RestClientException exception) {
				throw new ProjectException(ScheduleErrorCode.ROUTE_ESTIMATION_FAILED, exception);
			}
		}
		return fallbackRouteEstimator.estimate(mode, fromPlace, toPlace);
	}
}
