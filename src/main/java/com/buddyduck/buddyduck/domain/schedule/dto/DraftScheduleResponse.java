package com.buddyduck.buddyduck.domain.schedule.dto;

import java.util.List;

public record DraftScheduleResponse(
	String fitStatus,
	String recommendedStartAt,
	String effectiveStartAt,
	String targetArrivalAt,
	Integer overrunMinutes,
	Integer spareMinutes,
	List<DraftSlotResponse> slots,
	List<DraftRouteSegmentResponse> routeSegments,
	List<String> warnings
) {
}
