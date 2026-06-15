package com.buddyduck.buddyduck.domain.place.kakao.dto;

import java.util.List;

public record KakaoAddressSearchResponse(
	List<KakaoAddressDocument> documents
) {
}
