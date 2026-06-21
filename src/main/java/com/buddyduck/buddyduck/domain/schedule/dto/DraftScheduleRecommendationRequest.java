package com.buddyduck.buddyduck.domain.schedule.dto;

import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.OffsetDateTime;
import java.util.List;

public record DraftScheduleRecommendationRequest(
	@NotNull
	@PositiveOrZero
	Integer arrivalBufferMinutes,

	OffsetDateTime customStartAt,

	OffsetDateTime targetArrivalAt,

	@NotNull
	RouteMode recommendationMode,

	@Valid
	@NotEmpty
	List<DraftSlotRequest> slots
) {
}
