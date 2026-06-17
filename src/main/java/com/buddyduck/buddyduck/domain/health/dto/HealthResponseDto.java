package com.buddyduck.buddyduck.domain.health.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HealthResponseDto {

	private final String status;

	public static HealthResponseDto up() {
		return new HealthResponseDto("UP");
	}
}
