package com.buddyduck.buddyduck.domain.schedule.dto;

import java.time.LocalDateTime;

public record DraftSlotResponse(
	String clientId,
	Long slotId,
	Integer order,
	String title,
	Long placeId,
	Integer dwellMinutes,
	String startAt,
	String endAt,
	Boolean locked
) {

	public static DraftSlotResponse from(DraftSlotRequest request) {
		return from(request, null, null);
	}

	public static DraftSlotResponse from(DraftSlotRequest request, LocalDateTime startAt, LocalDateTime endAt) {
		return new DraftSlotResponse(
			request.clientId(),
			request.slotId(),
			request.order(),
			request.title(),
			request.placeId(),
			request.dwellMinutes(),
			ScheduleDateTimeFormatter.format(startAt),
			ScheduleDateTimeFormatter.format(endAt),
			Boolean.TRUE.equals(request.locked())
		);
	}
}
