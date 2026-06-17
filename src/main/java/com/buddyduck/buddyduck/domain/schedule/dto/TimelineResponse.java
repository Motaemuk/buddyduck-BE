package com.buddyduck.buddyduck.domain.schedule.dto;

import java.util.List;

public record TimelineResponse(
	TimelineRoomResponse room,
	TimelineScheduleResponse schedule,
	List<TimelineSlotResponse> slots,
	List<TimelineRouteSegmentResponse> routeSegments,
	List<String> warnings
) {
}
