package com.buddyduck.buddyduck.domain.concert.dto;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import java.math.BigDecimal;

public record ConcertSummaryResponse(
	Long id,
	String title,
	String venueName,
	String startAt,
	String endAt,
	BigDecimal lat,
	BigDecimal lng,
	String source
) {

	public static ConcertSummaryResponse from(Concert concert) {
		return new ConcertSummaryResponse(
			concert.getId(),
			concert.getTitle(),
			concert.getVenueName(),
			DateTimeResponseFormatter.format(concert.getStartAt()),
			DateTimeResponseFormatter.format(concert.getEndAt()),
			concert.getLat(),
			concert.getLng(),
			concert.getSource()
		);
	}
}
