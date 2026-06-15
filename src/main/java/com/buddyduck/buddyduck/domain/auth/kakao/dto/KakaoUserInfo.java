package com.buddyduck.buddyduck.domain.auth.kakao.dto;

import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;

public record KakaoUserInfo(
	String kakaoId,
	String nickname,
	AgeRange ageRange,
	UserGender gender
) {
}
