package com.buddyduck.buddyduck.domain.place.dto;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.kakao.KakaoLocalPlaceCandidate;
import java.math.BigDecimal;

public record PlaceSearchItemResponse(
	String providerPlaceId,
	String name,
	String address,
	BigDecimal lat,
	BigDecimal lng,
	String provider
) {

	public static PlaceSearchItemResponse from(Place place) {
		return new PlaceSearchItemResponse(
			place.getProviderPlaceId(),
			place.getName(),
			place.getAddress(),
			place.getLat(),
			place.getLng(),
			place.getProvider().name()
		);
	}

	public static PlaceSearchItemResponse from(KakaoLocalPlaceCandidate candidate) {
		return new PlaceSearchItemResponse(
			candidate.providerPlaceId(),
			candidate.name(),
			candidate.address(),
			candidate.lat(),
			candidate.lng(),
			candidate.provider().name()
		);
	}
}
