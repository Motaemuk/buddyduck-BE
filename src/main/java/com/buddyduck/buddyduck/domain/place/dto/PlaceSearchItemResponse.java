package com.buddyduck.buddyduck.domain.place.dto;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import java.math.BigDecimal;

public record PlaceSearchItemResponse(
	String name,
	String address,
	BigDecimal lat,
	BigDecimal lng,
	String provider
) {

	public static PlaceSearchItemResponse from(Place place) {
		return new PlaceSearchItemResponse(
			place.getName(),
			place.getAddress(),
			place.getLat(),
			place.getLng(),
			place.getProvider().name()
		);
	}
}
