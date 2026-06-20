package com.buddyduck.buddyduck.domain.schedule.dto;

public record TimelineScheduleResponse(
	Long id,
	Integer arrivalBufferMinutes,
	String timezone,
	String customStartAt,
	String targetArrivalAt,
	String recommendedStartAt,
	Integer overrunMinutes,
	Integer spareMinutes
) {
}
