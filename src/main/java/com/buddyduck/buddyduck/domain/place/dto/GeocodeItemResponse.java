package com.buddyduck.buddyduck.domain.place.dto;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.kakao.KakaoLocalPlaceCandidate;
import java.math.BigDecimal;

public record GeocodeItemResponse(
	String address,
	BigDecimal lat,
	BigDecimal lng,
	String provider
) {

	public static GeocodeItemResponse from(Place place) {
		return new GeocodeItemResponse(
			place.getAddress(),
			place.getLat(),
			place.getLng(),
			place.getProvider().name()
		);
	}

	public static GeocodeItemResponse from(KakaoLocalPlaceCandidate candidate) {
		return new GeocodeItemResponse(
			candidate.address(),
			candidate.lat(),
			candidate.lng(),
			candidate.provider().name()
		);
	}
}
