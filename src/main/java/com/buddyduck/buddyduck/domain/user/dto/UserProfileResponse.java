package com.buddyduck.buddyduck.domain.user.dto;

import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;

public record UserProfileResponse(
	Long id,
	String nickname,
	AgeRange ageRange,
	UserGender gender,
	String avatarColor
) {
}
