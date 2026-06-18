package com.buddyduck.buddyduck.domain.concert.kopis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "kopis.max-sync-rows=5")
class KopisConcertSyncServiceTest {

	@Autowired
	private KopisConcertSyncService kopisConcertSyncService;

	@Autowired
	private ConcertRepository concertRepository;

	@MockBean
	private KopisConcertClient kopisConcertClient;

	@BeforeEach
	void setUp() {
		concertRepository.deleteAll();
		given(kopisConcertClient.isEnabled()).willReturn(true);
	}

	@Test
	void KOPIS_공연_후보를_콘서트로_upsert한다() {
		given(kopisConcertClient.fetchConcerts(
			LocalDate.of(2026, 6, 1),
			LocalDate.of(2026, 6, 30),
			0,
			5,
			"AURORA"
		)).willReturn(List.of(candidate("AURORA LIVE")));

		int syncedCount = kopisConcertSyncService.syncConcerts(
			"AURORA",
			LocalDate.of(2026, 6, 1),
			LocalDate.of(2026, 6, 30),
			0,
			20
		);

		assertThat(syncedCount).isEqualTo(1);
		Concert concert = concertRepository.findBySourceAndExternalId("KOPIS", "PF178134").orElseThrow();
		assertThat(concert.getTitle()).isEqualTo("AURORA LIVE");
		assertThat(concert.getVenueName()).isEqualTo("KSPO Dome");
		assertThat(concert.getLat()).isEqualByComparingTo(new BigDecimal("37.5211200"));
	}

	@Test
	void 이미_있는_KOPIS_공연은_갱신한다() {
		concertRepository.save(Concert.create(
			"PF178134",
			"OLD TITLE",
			"OLD VENUE",
			LocalDateTime.of(2026, 6, 1, 0, 0),
			LocalDateTime.of(2026, 6, 1, 23, 59, 59),
			new BigDecimal("37.0000000"),
			new BigDecimal("127.0000000"),
			"KOPIS"
		));
		given(kopisConcertClient.fetchConcerts(
			LocalDate.of(2026, 6, 1),
			LocalDate.of(2026, 6, 30),
			0,
			5,
			null
		)).willReturn(List.of(candidate("UPDATED LIVE")));

		kopisConcertSyncService.syncConcerts(null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 0, 10);

		Concert concert = concertRepository.findBySourceAndExternalId("KOPIS", "PF178134").orElseThrow();
		assertThat(concert.getTitle()).isEqualTo("UPDATED LIVE");
		assertThat(concert.getVenueName()).isEqualTo("KSPO Dome");
	}

	private KopisConcertCandidate candidate(String title) {
		return new KopisConcertCandidate(
			"PF178134",
			title,
			"KSPO Dome",
			LocalDateTime.of(2026, 6, 20, 0, 0),
			LocalDateTime.of(2026, 6, 20, 23, 59, 59),
			new BigDecimal("37.5211200"),
			new BigDecimal("127.1283636")
		);
	}
}
