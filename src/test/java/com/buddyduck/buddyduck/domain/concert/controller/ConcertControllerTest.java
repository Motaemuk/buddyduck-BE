package com.buddyduck.buddyduck.domain.concert.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

import com.buddyduck.buddyduck.domain.concert.kopis.KopisConcertClient;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.security.AuthUser;
import com.buddyduck.buddyduck.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ConcertControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockBean
	private KopisConcertClient kopisConcertClient;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM concert_interest_tags");
		jdbcTemplate.update("DELETE FROM rooms");
		jdbcTemplate.update("DELETE FROM places");
		jdbcTemplate.update("DELETE FROM concerts");
		userRepository.deleteAll();
	}

	@Test
	void 공연_목록은_인증_없이_다가오는_공연순으로_조회한다() throws Exception {
		LocalDateTime earlyStartAt = futureConcertStartAt(1);
		LocalDateTime lateStartAt = futureConcertStartAt(5);
		insertConcert("seed-late", "BLOSSOM LIVE", "KSPO Dome", lateStartAt);
		Long concertId = insertConcert("seed-early", "AURORA LIVE", "Jamsil Arena", earlyStartAt);
		insertOpenRoom(concertId);

		mockMvc.perform(get("/api/concerts")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.items[0].title").value("AURORA LIVE"))
			.andExpect(jsonPath("$.result.items[0].venueName").value("Jamsil Arena"))
			.andExpect(jsonPath("$.result.items[0].startAt").value(formatDateTime(earlyStartAt)))
			.andExpect(jsonPath("$.result.items[0].source").value("SEED"))
			.andExpect(jsonPath("$.result.items[0].posterUrl").value("https://example.com/poster.png"))
			.andExpect(jsonPath("$.result.items[0].area").value("서울"))
			.andExpect(jsonPath("$.result.items[0].genre").value("K-POP"))
			.andExpect(jsonPath("$.result.items[0].timeGuidance").value("월요일(19:00)"))
			.andExpect(jsonPath("$.result.items[0].openRoomCount").value(1))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(20))
			.andExpect(jsonPath("$.result.hasNext").value(false));

		verifyNoInteractions(kopisConcertClient);
	}

	@Test
	void 공연_목록은_종료된_공연을_제외한다() throws Exception {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
		insertConcert("seed-expired", "YESTERDAY LIVE", "Old Arena", now.minusDays(2), now.minusDays(1));
		insertConcert("seed-active", "TOMORROW LIVE", "KSPO Dome", now.plusDays(1), now.plusDays(1).plusHours(2));

		mockMvc.perform(get("/api/concerts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items.length()").value(1))
			.andExpect(jsonPath("$.result.items[0].title").value("TOMORROW LIVE"));
	}

	@Test
	void 공연_목록은_keyword와_region으로_필터링한다() throws Exception {
		insertConcert("seed-1", "AURORA LIVE", "Jamsil Arena", futureConcertStartAt(1));
		insertConcert("seed-2", "BLOSSOM LIVE", "Busan Dome", futureConcertStartAt(5));

		mockMvc.perform(get("/api/concerts")
				.param("keyword", "AURORA")
				.param("region", "Jamsil"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items.length()").value(1))
			.andExpect(jsonPath("$.result.items[0].title").value("AURORA LIVE"));
	}

	@Test
	void 공연_상세를_조회한다() throws Exception {
		Long concertId = insertConcert("seed-detail", "AURORA LIVE", "KSPO Dome", LocalDateTime.of(2026, 6, 15, 19, 0));

		mockMvc.perform(get("/api/concerts/{concertId}", concertId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.id").value(concertId))
			.andExpect(jsonPath("$.result.title").value("AURORA LIVE"))
			.andExpect(jsonPath("$.result.venueName").value("KSPO Dome"))
			.andExpect(jsonPath("$.result.startAt").value("2026-06-15T19:00:00+09:00"))
			.andExpect(jsonPath("$.result.endAt").value("2026-06-15T21:00:00+09:00"))
			.andExpect(jsonPath("$.result.lat").value(37.519))
			.andExpect(jsonPath("$.result.lng").value(127.127))
			.andExpect(jsonPath("$.result.posterUrl").value("https://example.com/poster.png"))
			.andExpect(jsonPath("$.result.area").value("서울"))
			.andExpect(jsonPath("$.result.genre").value("K-POP"))
			.andExpect(jsonPath("$.result.timeGuidance").value("월요일(19:00)"))
			.andExpect(jsonPath("$.result.openRoomCount").value(0));
	}

	@Test
	void 내_공연별_관심_태그를_조회한다() throws Exception {
		Long concertId = insertConcert("seed-tags", "AURORA LIVE", "KSPO Dome", LocalDateTime.of(2026, 6, 15, 19, 0));
		User user = saveCompletedUser("duck_fan");
		insertInterestTag(user.getId(), concertId, "GOODS_BUYING");
		insertInterestTag(user.getId(), concertId, "CAFE_VISIT");

		mockMvc.perform(get("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(user)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tags[0]").value("GOODS_BUYING"))
			.andExpect(jsonPath("$.result.tags[1]").value("CAFE_VISIT"));
	}

	@Test
	void 내_공연별_관심_태그를_저장하면_기존_태그를_대체한다() throws Exception {
		Long concertId = insertConcert("seed-save-tags", "AURORA LIVE", "KSPO Dome", LocalDateTime.of(2026, 6, 15, 19, 0));
		User user = saveCompletedUser("duck_fan");
		insertInterestTag(user.getId(), concertId, "GOODS_BUYING");

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"tags", new String[] {"CAFE_VISIT", "MEAL_TOGETHER"}
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tags[0]").value("CAFE_VISIT"))
			.andExpect(jsonPath("$.result.tags[1]").value("MEAL_TOGETHER"));

		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM concert_interest_tags WHERE user_id = ? AND concert_id = ?",
			Integer.class,
			user.getId(),
			concertId
		);
		assertThat(count).isEqualTo(2);
	}

	@Test
	void 이미_저장된_관심_태그를_다시_저장해도_성공한다() throws Exception {
		Long concertId = insertConcert("seed-save-same-tags", "AURORA LIVE", "KSPO Dome", LocalDateTime.of(2026, 6, 15, 19, 0));
		User user = saveCompletedUser("duck_fan");
		insertInterestTag(user.getId(), concertId, "GOODS_BUYING");
		insertInterestTag(user.getId(), concertId, "CAFE_VISIT");

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"tags", new String[] {"GOODS_BUYING", "CAFE_VISIT"}
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tags[0]").value("GOODS_BUYING"))
			.andExpect(jsonPath("$.result.tags[1]").value("CAFE_VISIT"));

		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM concert_interest_tags WHERE user_id = ? AND concert_id = ?",
			Integer.class,
			user.getId(),
			concertId
		);
		assertThat(count).isEqualTo(2);
	}

	@Test
	void 허용되지_않은_관심_태그는_400을_응답한다() throws Exception {
		Long concertId = insertConcert("seed-invalid-tags", "AURORA LIVE", "KSPO Dome", LocalDateTime.of(2026, 6, 15, 19, 0));
		User user = saveCompletedUser("duck_fan");

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"tags", new String[] {"INVALID_TAG"}
				))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	@Test
	void null_관심_태그는_400을_응답한다() throws Exception {
		Long concertId = insertConcert("seed-null-tags", "AURORA LIVE", "KSPO Dome", LocalDateTime.of(2026, 6, 15, 19, 0));
		User user = saveCompletedUser("duck_fan");

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(user))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"tags":[null]}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	@Test
	void 공연_seed를_생성한다() throws Exception {
		mockMvc.perform(post("/api/dev/seed/concerts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.result.createdConcertCount").isNumber());
	}

	private Long insertConcert(String externalId, String title, String venueName, LocalDateTime startAt) {
		return insertConcert(externalId, title, venueName, startAt, startAt.plusHours(2));
	}

	private Long insertConcert(
		String externalId,
		String title,
		String venueName,
		LocalDateTime startAt,
		LocalDateTime endAt
	) {
		jdbcTemplate.update("""
			INSERT INTO concerts (
				external_id, title, venue_name, start_at, end_at, lat, lng, source,
				poster_url, area, genre, time_guidance, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			externalId,
			title,
			venueName,
			startAt,
			endAt,
			new BigDecimal("37.5190000"),
			new BigDecimal("127.1270000"),
			"SEED",
			"https://example.com/poster.png",
			"서울",
			"K-POP",
			"월요일(19:00)",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM concerts WHERE external_id = ?", Long.class, externalId);
	}

	private LocalDateTime futureConcertStartAt(int daysAfterToday) {
		return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
			.plusDays(daysAfterToday)
			.withHour(19)
			.withMinute(0)
			.withSecond(0)
			.withNano(0);
	}

	private String formatDateTime(LocalDateTime value) {
		return value.atOffset(ZoneOffset.ofHours(9)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	private void insertOpenRoom(Long concertId) {
		User host = saveCompletedUser("host_" + concertId);
		Long placeId = insertPlace("place-" + concertId);
		jdbcTemplate.update("""
			INSERT INTO rooms (
				concert_id, host_user_id, title, description, max_members, meeting_at,
				meeting_place_id, event_place_id, open_chat_url, status, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			concertId,
			host.getId(),
			"같이 가요",
			"공연 같이 갈 사람",
			4,
			LocalDateTime.of(2026, 6, 15, 17, 0),
			placeId,
			placeId,
			"https://open.kakao.com/o/test",
			"OPEN",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
	}

	private Long insertPlace(String providerPlaceId) {
		jdbcTemplate.update("""
			INSERT INTO places (provider, provider_place_id, name, address, lat, lng, created_at, updated_at)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""",
			"KAKAO_KEYWORD",
			providerPlaceId,
			"KSPO Dome",
			"서울 송파구 올림픽로 424",
			new BigDecimal("37.5190000"),
			new BigDecimal("127.1270000"),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM places WHERE provider_place_id = ?",
			Long.class,
			providerPlaceId
		);
	}

	private User saveCompletedUser(String nickname) {
		User user = User.createKakao("kakao-" + nickname, nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile(nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		return userRepository.save(user);
	}

	private void insertInterestTag(Long userId, Long concertId, String tag) {
		jdbcTemplate.update(
			"INSERT INTO concert_interest_tags (user_id, concert_id, tag) VALUES (?, ?, ?)",
			userId,
			concertId,
			tag
		);
	}

	private String bearer(User user) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));
	}
}
