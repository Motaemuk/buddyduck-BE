package com.buddyduck.buddyduck.domain.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record DraftScheduleRequest(
	@NotNull
	@PositiveOrZero
	Integer arrivalBufferMinutes,

	@Valid
	@NotEmpty
	List<DraftSlotRequest> slots,

	@Valid
	@NotNull
	List<DraftRouteSegmentRequest> routeSegments
) {
}
