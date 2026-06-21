package com.buddyduck.buddyduck.domain.schedule.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RouteSegmentResponseContractTest {

	@Test
	void 일정_이동구간_public_응답에는_요금_필드를_노출하지_않는다() {
		assertThat(recordComponentNames(DraftRouteSegmentResponse.class))
			.doesNotContain("taxiFareWon", "tollFareWon");
		assertThat(recordComponentNames(TimelineRouteSegmentResponse.class))
			.doesNotContain("taxiFareWon", "tollFareWon");
	}

	private static String[] recordComponentNames(Class<? extends Record> responseType) {
		return Arrays.stream(responseType.getRecordComponents())
			.map(RecordComponent::getName)
			.toArray(String[]::new);
	}
}
