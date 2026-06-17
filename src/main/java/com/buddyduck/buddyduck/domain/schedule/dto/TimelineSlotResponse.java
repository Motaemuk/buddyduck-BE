package com.buddyduck.buddyduck.domain.schedule.dto;

import com.buddyduck.buddyduck.domain.schedule.entity.ScheduleSlot;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotCategory;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotType;
import java.math.BigDecimal;

public record TimelineSlotResponse(
	Long slotId,
	Integer order,
	String title,
	Long placeId,
	String placeName,
	BigDecimal lat,
	BigDecimal lng,
	SlotType slotType,
	SlotCategory category,
	String startAt,
	String endAt,
	Integer dwellMinutes,
	Boolean locked
) {

	public static TimelineSlotResponse from(ScheduleSlot slot) {
		return new TimelineSlotResponse(
			slot.getId(),
			slot.getSortOrder(),
			slot.getTitle(),
			slot.getPlace() == null ? null : slot.getPlace().getId(),
			slot.getPlace() == null ? null : slot.getPlace().getName(),
			slot.getPlace() == null ? null : slot.getPlace().getLat(),
			slot.getPlace() == null ? null : slot.getPlace().getLng(),
			slot.getSlotType(),
			slot.getCategory(),
			ScheduleDateTimeFormatter.format(slot.getStartAt()),
			ScheduleDateTimeFormatter.format(slot.getEndAt()),
			slot.getDwellMinutes(),
			slot.getLocked()
		);
	}
}
