package com.buddyduck.buddyduck.domain.schedule.dto;

import java.util.List;

public record DraftScheduleResponse(
	String fitStatus,
	Integer overrunMinutes,
	List<DraftSlotResponse> slots,
	List<DraftRouteSegmentResponse> routeSegments,
	List<String> warnings
) {
}
