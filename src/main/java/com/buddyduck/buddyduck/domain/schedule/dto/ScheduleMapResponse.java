package com.buddyduck.buddyduck.domain.schedule.dto;

import java.util.List;

public record ScheduleMapResponse(
	List<TimelineSlotResponse> slots,
	List<TimelineRouteSegmentResponse> routeSegments,
	MapBoundsResponse mapBounds
) {
}
