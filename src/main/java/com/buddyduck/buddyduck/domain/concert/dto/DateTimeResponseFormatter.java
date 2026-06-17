package com.buddyduck.buddyduck.domain.concert.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

final class DateTimeResponseFormatter {

	private static final ZoneOffset SEOUL_OFFSET = ZoneOffset.ofHours(9);

	private DateTimeResponseFormatter() {
	}

	static String format(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.atOffset(SEOUL_OFFSET).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
