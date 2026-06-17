package com.buddyduck.buddyduck.domain.concert.dto;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InterestTagRequest(
	@NotNull
	List<@NotNull InterestTag> tags
) {
}
