package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import java.util.List;

public record JoinRequestListItemResponse(
	Long requestId,
	Long userId,
	String nickname,
	AgeRange ageRange,
	UserGender gender,
	String message,
	List<InterestTag> matchedTags,
	String createdAt
) {
}
