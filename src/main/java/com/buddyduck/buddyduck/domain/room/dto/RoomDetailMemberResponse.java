package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;

public record RoomDetailMemberResponse(
	Long userId,
	String nickname,
	AgeRange ageRange,
	UserGender gender,
	String role,
	int sharedInterestCount
) {
}
