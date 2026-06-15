package com.buddyduck.buddyduck.domain.place.kakao;

import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import java.math.BigDecimal;

public record KakaoLocalPlaceCandidate(
	PlaceSource provider,
	String providerPlaceId,
	String name,
	String address,
	BigDecimal lat,
	BigDecimal lng
) {
}
