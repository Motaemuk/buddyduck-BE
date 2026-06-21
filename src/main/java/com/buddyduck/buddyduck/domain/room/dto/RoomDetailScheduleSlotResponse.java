package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.schedule.enums.SlotCategory;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotType;

public record RoomDetailScheduleSlotResponse(
	Long slotId,
	Integer order,
	String title,
	Long placeId,
	String placeName,
	SlotType slotType,
	SlotCategory category,
	String startAt,
	String endAt,
	Integer dwellMinutes,
	Boolean locked
) {
}
