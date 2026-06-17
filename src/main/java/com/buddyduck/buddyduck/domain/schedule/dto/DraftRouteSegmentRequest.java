package com.buddyduck.buddyduck.domain.schedule.dto;

import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DraftRouteSegmentRequest(
	@NotBlank
	String fromClientId,

	@NotBlank
	String toClientId,

	@NotNull
	RouteMode mode,

	@NotNull
	@PositiveOrZero
	Integer durationMinutes
) {
}
