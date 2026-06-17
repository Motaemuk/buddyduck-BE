package com.buddyduck.buddyduck.domain.concert.service;

import com.buddyduck.buddyduck.domain.concert.dto.SeedConcertsResponse;
import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConcertSeedService {

	private static final String SOURCE = "SEED";

	private final ConcertRepository concertRepository;

	@Transactional
	public SeedConcertsResponse seedConcerts() {
		int createdCount = 0;
		for (ConcertSeed seed : seeds()) {
			if (concertRepository.existsBySourceAndExternalId(SOURCE, seed.externalId())) {
				continue;
			}
			try {
				concertRepository.saveAndFlush(Concert.create(
					seed.externalId(),
					seed.title(),
					seed.venueName(),
					seed.startAt(),
					seed.endAt(),
					seed.lat(),
					seed.lng(),
					SOURCE
				));
				createdCount++;
			} catch (DataIntegrityViolationException ignored) {
				// Another request inserted the same seed between the existence check and save.
			}
		}
		return new SeedConcertsResponse(createdCount);
	}

	private List<ConcertSeed> seeds() {
		return List.of(
			new ConcertSeed(
				"seed-aurora-live",
				"AURORA LIVE",
				"KSPO Dome",
				LocalDateTime.of(2026, 6, 15, 19, 0),
				LocalDateTime.of(2026, 6, 15, 21, 30),
				new BigDecimal("37.5190000"),
				new BigDecimal("127.1270000")
			),
			new ConcertSeed(
				"seed-moonlight-festa",
				"MOONLIGHT FESTA",
				"Jamsil Arena",
				LocalDateTime.of(2026, 6, 21, 18, 30),
				LocalDateTime.of(2026, 6, 21, 21, 0),
				new BigDecimal("37.5111000"),
				new BigDecimal("127.0719000")
			),
			new ConcertSeed(
				"seed-busan-wave",
				"BUSAN WAVE",
				"Busan Dome",
				LocalDateTime.of(2026, 7, 3, 19, 30),
				LocalDateTime.of(2026, 7, 3, 22, 0),
				new BigDecimal("35.1689000"),
				new BigDecimal("129.1366000")
			)
		);
	}

	private record ConcertSeed(
		String externalId,
		String title,
		String venueName,
		LocalDateTime startAt,
		LocalDateTime endAt,
		BigDecimal lat,
		BigDecimal lng
	) {
	}
}
