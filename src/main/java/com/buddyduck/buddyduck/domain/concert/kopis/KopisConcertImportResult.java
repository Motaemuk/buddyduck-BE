package com.buddyduck.buddyduck.domain.concert.kopis;

import java.time.LocalDate;

public record KopisConcertImportResult(
	LocalDate from,
	LocalDate to,
	int pages,
	int fetchedCount,
	int syncedCount
) {
}
