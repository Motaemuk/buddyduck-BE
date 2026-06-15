package com.buddyduck.buddyduck.domain.auth.kakao;

import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;

public interface KakaoAuthClient {

	KakaoUserInfo getUserInfo(String code, String redirectUri);
}
