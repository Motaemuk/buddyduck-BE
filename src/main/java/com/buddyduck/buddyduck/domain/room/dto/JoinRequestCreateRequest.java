package com.buddyduck.buddyduck.domain.room.dto;

import jakarta.validation.constraints.Size;

public record JoinRequestCreateRequest(
	@Size(max = 300)
	String message
) {
}
