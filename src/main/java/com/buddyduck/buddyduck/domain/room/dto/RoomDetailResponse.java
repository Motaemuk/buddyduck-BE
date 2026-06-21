package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import java.util.List;

public record RoomDetailResponse(
	Long id,
	String title,
	String description,
	String roomStatus,
	String viewerRole,
	String viewerJoinStatus,
	RoomPermissionsResponse permissions,
	long pendingRequestCount,
	RoomDetailConcertResponse concert,
	String meetingAt,
	String meetingPlaceName,
	String meetingPlaceAddress,
	List<InterestTag> roomTags,
	long memberCount,
	Integer maxMembers,
	List<RoomDetailMemberResponse> members,
	List<RoomDetailScheduleSlotResponse> schedulePreview
) {
}
