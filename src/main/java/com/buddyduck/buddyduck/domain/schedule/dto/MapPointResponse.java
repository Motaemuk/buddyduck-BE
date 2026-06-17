package com.buddyduck.buddyduck.domain.schedule.dto;

import java.math.BigDecimal;

public record MapPointResponse(
	BigDecimal lat,
	BigDecimal lng
) {
}
