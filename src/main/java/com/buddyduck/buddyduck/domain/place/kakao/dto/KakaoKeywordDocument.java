package com.buddyduck.buddyduck.domain.place.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoKeywordDocument(
	String id,

	@JsonProperty("place_name")
	String placeName,

	@JsonProperty("address_name")
	String addressName,

	@JsonProperty("road_address_name")
	String roadAddressName,

	String x,

	String y
) {
}
