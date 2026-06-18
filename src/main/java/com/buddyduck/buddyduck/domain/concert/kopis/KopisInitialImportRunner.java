package com.buddyduck.buddyduck.domain.concert.kopis;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kopis.initial-import", name = "enabled", havingValue = "true")
public class KopisInitialImportRunner implements ApplicationRunner {

	private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

	private final KopisConcertSyncService kopisConcertSyncService;
	private final KopisProperties kopisProperties;

	@Override
	public void run(ApplicationArguments args) {
		KopisProperties.InitialImport importProperties = kopisProperties.getInitialImport();
		LocalDate from = importProperties.getFrom() == null
			? LocalDate.now(SERVICE_ZONE)
			: importProperties.getFrom();
		LocalDate to = from.plusDays(Math.max(0, importProperties.getDays()));

		KopisConcertImportResult result = kopisConcertSyncService.importConcerts(from, to);
		log.info(
			"KOPIS initial import finished. from={}, to={}, pages={}, fetchedCount={}, syncedCount={}",
			result.from(),
			result.to(),
			result.pages(),
			result.fetchedCount(),
			result.syncedCount()
		);
	}
}
