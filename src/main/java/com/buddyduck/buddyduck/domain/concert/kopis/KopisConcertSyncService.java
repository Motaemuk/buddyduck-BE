package com.buddyduck.buddyduck.domain.concert.kopis;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KopisConcertSyncService {

	private static final String SOURCE = "KOPIS";
	private static final int KOPIS_MAX_RANGE_DAYS = 30;
	private static final int KOPIS_MAX_ROWS = 100;
	private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

	private final KopisConcertClient kopisConcertClient;
	private final KopisProperties kopisProperties;
	private final ConcertRepository concertRepository;

	public int syncConcerts(String keyword, LocalDate from, LocalDate to, int page, int size) {
		if (!kopisConcertClient.isEnabled()) {
			return 0;
		}

		LocalDate startDate = from == null ? LocalDate.now(SERVICE_ZONE) : from;
		LocalDate endDate = normalizeEndDate(startDate, to);
		int rows = normalizeRows(size);

		try {
			List<KopisConcertCandidate> candidates = kopisConcertClient.fetchConcerts(
				startDate,
				endDate,
				page,
				rows,
				keyword
			);
			return upsert(candidates);
		} catch (IllegalArgumentException | RestClientException exception) {
			log.warn(
				"Failed to sync KOPIS concerts. keyword={}, from={}, to={}",
				keyword,
				startDate,
				endDate,
				exception
			);
			return 0;
		}
	}

	private LocalDate normalizeEndDate(LocalDate startDate, LocalDate requestedEndDate) {
		if (requestedEndDate == null || requestedEndDate.isBefore(startDate)) {
			return startDate.plusDays(KOPIS_MAX_RANGE_DAYS);
		}
		long days = ChronoUnit.DAYS.between(startDate, requestedEndDate);
		if (days > KOPIS_MAX_RANGE_DAYS) {
			return startDate.plusDays(KOPIS_MAX_RANGE_DAYS);
		}
		return requestedEndDate;
	}

	private int normalizeRows(int size) {
		int configuredLimit = Math.max(1, Math.min(kopisProperties.getMaxSyncRows(), KOPIS_MAX_ROWS));
		return Math.max(1, Math.min(size, configuredLimit));
	}

	private int upsert(List<KopisConcertCandidate> candidates) {
		int syncedCount = 0;
		for (KopisConcertCandidate candidate : candidates) {
			if (upsert(candidate)) {
				syncedCount++;
			}
		}
		return syncedCount;
	}

	private boolean upsert(KopisConcertCandidate candidate) {
		try {
			Concert concert = concertRepository.findBySourceAndExternalId(SOURCE, candidate.externalId())
				.map(existing -> update(existing, candidate))
				.orElseGet(() -> create(candidate));
			concert.updateCardMetadata(
				candidate.posterUrl(),
				candidate.area(),
				candidate.genre(),
				candidate.timeGuidance()
			);
			concertRepository.save(concert);
			return true;
		} catch (DataIntegrityViolationException exception) {
			log.debug("KOPIS concert was inserted concurrently. externalId={}", candidate.externalId());
			return false;
		}
	}

	private Concert update(Concert concert, KopisConcertCandidate candidate) {
		concert.updateDetails(
			candidate.title(),
			candidate.venueName(),
			candidate.startAt(),
			candidate.endAt(),
			candidate.lat(),
			candidate.lng()
		);
		return concert;
	}

	private Concert create(KopisConcertCandidate candidate) {
		return Concert.create(
			candidate.externalId(),
			candidate.title(),
			candidate.venueName(),
			candidate.startAt(),
			candidate.endAt(),
			candidate.lat(),
			candidate.lng(),
			SOURCE
		);
	}
}
