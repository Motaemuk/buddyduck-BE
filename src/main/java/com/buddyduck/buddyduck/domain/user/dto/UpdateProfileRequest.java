package com.buddyduck.buddyduck.domain.user.dto;

import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
	@NotBlank
	@Size(min = 2, max = 12)
	@Pattern(regexp = "^[가-힣a-zA-Z0-9_-]+$")
	String nickname,

	@NotNull
	AgeRange ageRange,

	@NotNull
	UserGender gender,

	@NotNull
	Boolean ageVisible,

	@NotNull
	Boolean genderVisible
) {
}
