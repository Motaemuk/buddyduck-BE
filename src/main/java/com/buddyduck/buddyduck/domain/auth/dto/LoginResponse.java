package com.buddyduck.buddyduck.domain.auth.dto;

public record LoginResponse(
	String accessToken,
	boolean isNewUser,
	boolean profileCompleted,
	LoginUserSummary user
) {
}
