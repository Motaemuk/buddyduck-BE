package com.buddyduck.buddyduck.domain.concert.kopis;

import java.time.LocalDate;

record KopisConcertDetail(
	String externalId,
	String title,
	String venueName,
	String facilityId,
	LocalDate startDate,
	LocalDate endDate
) {
}
