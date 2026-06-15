package com.buddyduck.buddyduck.domain.schedule.dto;

public record DraftSlotResponse(
	String clientId,
	Long slotId,
	Integer order,
	String title,
	Long placeId,
	Integer dwellMinutes,
	Boolean locked
) {

	public static DraftSlotResponse from(DraftSlotRequest request) {
		return new DraftSlotResponse(
			request.clientId(),
			request.slotId(),
			request.order(),
			request.title(),
			request.placeId(),
			request.dwellMinutes(),
			Boolean.TRUE.equals(request.locked())
		);
	}
}
