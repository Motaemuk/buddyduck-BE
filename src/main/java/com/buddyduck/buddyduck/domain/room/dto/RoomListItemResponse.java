package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import java.util.List;

public record RoomListItemResponse(
	Long id,
	String title,
	String hostNickname,
	String status,
	boolean isFull,
	long memberCount,
	Integer maxMembers,
	String meetingAt,
	String meetingPlaceName,
	List<InterestTag> roomTags,
	int matchCount
) {
}
