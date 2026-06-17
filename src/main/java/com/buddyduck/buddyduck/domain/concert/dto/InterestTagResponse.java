package com.buddyduck.buddyduck.domain.concert.dto;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import java.util.List;

public record InterestTagResponse(
	List<InterestTag> tags
) {
}
