package com.buddyduck.buddyduck.domain.concert.kopis;

import java.time.LocalDate;
import java.util.List;

public interface KopisConcertClient {

	boolean isEnabled();

	List<KopisConcertCandidate> fetchConcerts(
		LocalDate from,
		LocalDate to,
		int page,
		int rows,
		String keyword
	);
}
