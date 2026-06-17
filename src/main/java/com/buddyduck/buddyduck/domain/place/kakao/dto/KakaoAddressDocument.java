package com.buddyduck.buddyduck.domain.place.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoAddressDocument(
	@JsonProperty("address_name")
	String addressName,

	String x,

	String y
) {
}
