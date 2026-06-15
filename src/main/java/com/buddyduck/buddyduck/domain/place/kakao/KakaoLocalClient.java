package com.buddyduck.buddyduck.domain.place.kakao;

import java.util.List;

public interface KakaoLocalClient {

	boolean isEnabled();

	List<KakaoLocalPlaceCandidate> searchKeyword(String keyword);

	List<KakaoLocalPlaceCandidate> searchAddress(String address);
}
