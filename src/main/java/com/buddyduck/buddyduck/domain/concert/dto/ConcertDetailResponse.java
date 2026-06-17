package com.buddyduck.buddyduck.domain.concert.dto;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import java.math.BigDecimal;

public record ConcertDetailResponse(
	Long id,
	String title,
	String venueName,
	String startAt,
	BigDecimal lat,
	BigDecimal lng
) {

	public static ConcertDetailResponse from(Concert concert) {
		return new ConcertDetailResponse(
			concert.getId(),
			concert.getTitle(),
			concert.getVenueName(),
			DateTimeResponseFormatter.format(concert.getStartAt()),
			concert.getLat(),
			concert.getLng()
		);
	}
}
