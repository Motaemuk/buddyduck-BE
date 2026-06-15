package com.buddyduck.buddyduck.domain.place.dto;

import com.buddyduck.buddyduck.domain.place.entity.Place;
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
}
