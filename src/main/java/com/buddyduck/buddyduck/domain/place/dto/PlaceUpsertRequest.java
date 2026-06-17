package com.buddyduck.buddyduck.domain.place.dto;

import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PlaceUpsertRequest(
	@NotNull
	PlaceSource provider,

	String providerPlaceId,

	@NotBlank
	String name,

	@NotBlank
	String address,

	@NotNull
	BigDecimal lat,

	@NotNull
	BigDecimal lng
) {
}
