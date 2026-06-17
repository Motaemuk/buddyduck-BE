package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RoomPlaceRequest(
	@NotNull
	PlaceSource provider,

	String providerPlaceId,

	@NotBlank
	String name,

	@NotBlank
	String address,

	@NotNull
	@DecimalMin("-90.0")
	@DecimalMax("90.0")
	BigDecimal lat,

	@NotNull
	@DecimalMin("-180.0")
	@DecimalMax("180.0")
	BigDecimal lng
) {
}
