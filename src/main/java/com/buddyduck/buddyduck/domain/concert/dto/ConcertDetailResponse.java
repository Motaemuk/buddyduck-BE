package com.buddyduck.buddyduck.domain.concert.dto;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import java.math.BigDecimal;

public record ConcertDetailResponse(
	Long id,
	String title,
	String venueName,
	String startAt,
	String endAt,
	BigDecimal lat,
	BigDecimal lng,
	String source,
	String posterUrl,
	String area,
	String genre,
	String timeGuidance,
	long openRoomCount
) {

	public static ConcertDetailResponse from(Concert concert, long openRoomCount) {
		return new ConcertDetailResponse(
			concert.getId(),
			concert.getTitle(),
			concert.getVenueName(),
			DateTimeResponseFormatter.format(concert.getStartAt()),
			DateTimeResponseFormatter.format(concert.getEndAt()),
			concert.getLat(),
			concert.getLng(),
			concert.getSource(),
			concert.getPosterUrl(),
			concert.getArea(),
			concert.getGenre(),
			concert.getTimeGuidance(),
			openRoomCount
		);
	}
}
