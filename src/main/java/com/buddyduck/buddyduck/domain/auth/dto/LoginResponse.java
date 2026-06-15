package com.buddyduck.buddyduck.domain.auth.dto;

public record LoginResponse(
	String accessToken,
	boolean isNewUser,
	LoginUserSummary user
) {
}
