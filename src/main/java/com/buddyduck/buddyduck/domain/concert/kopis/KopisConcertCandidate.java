package com.buddyduck.buddyduck.domain.concert.kopis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KopisConcertCandidate(
	String externalId,
	String title,
	String venueName,
	LocalDateTime startAt,
	LocalDateTime endAt,
	BigDecimal lat,
	BigDecimal lng,
	String posterUrl,
	String area,
	String genre,
	String timeGuidance
) {
}
