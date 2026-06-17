package com.buddyduck.buddyduck.domain.schedule.dto;

import com.buddyduck.buddyduck.domain.schedule.enums.SlotCategory;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record DraftSlotRequest(
	@NotBlank
	String clientId,

	Long slotId,

	@NotNull
	@Positive
	Integer order,

	@NotBlank
	String title,

	Long placeId,

	@NotNull
	@PositiveOrZero
	Integer dwellMinutes,

	Boolean locked,

	SlotType slotType,

	SlotCategory category
) {
}
