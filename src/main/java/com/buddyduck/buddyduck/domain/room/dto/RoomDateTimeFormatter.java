package com.buddyduck.buddyduck.domain.room.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class RoomDateTimeFormatter {

	private static final ZoneOffset SEOUL_OFFSET = ZoneOffset.ofHours(9);

	private RoomDateTimeFormatter() {
	}

	public static String format(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.atOffset(SEOUL_OFFSET).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
