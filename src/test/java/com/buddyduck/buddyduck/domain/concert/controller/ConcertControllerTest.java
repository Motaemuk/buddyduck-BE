package com.buddyduck.buddyduck.domain.concert.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.security.AuthUser;
import com.buddyduck.buddyduck.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM concert_interest_tags");
		jdbcTemplate.update("DELETE FROM concerts");
		userRepository.deleteAll();
	}

	@Test
	void 공연_목록은_인증_없이_다가오는_공연순으로_조회한다() throws Exception {
		insertConcert("seed-late", "BLOSSOM LIVE", "KSPO Dome", LocalDateTime.of(2026, 6, 20, 19, 0));
		insertConcert("seed-early", "AURORA LIVE", "Jamsil Arena", LocalDateTime.of(2026, 6, 15, 19, 0));

		mockMvc.perform(get("/api/concerts")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.items[0].title").value("AURORA LIVE"))
			.andExpect(jsonPath("$.result.items[0].venueName").value("Jamsil Arena"))
			.andExpect(jsonPath("$.result.items[0].startAt").value("2026-06-15T19:00:00+09:00"))
			.andExpect(jsonPath("$.result.items[0].source").value("SEED"))
			.andExpect(jsonPath("$.result.page").value(0))
			.andExpect(jsonPath("$.result.size").value(20))
			.andExpect(jsonPath("$.result.hasNext").value(false));
	}

	@Test
	void 공연_목록은_keyword와_region으로_필터링한다() throws Exception {
		insertConcert("seed-1", "AURORA LIVE", "Jamsil Arena", LocalDateTime.of(2026, 6, 15, 19, 0));
		insertConcert("seed-2", "BLOSSOM LIVE", "Busan Dome", LocalDateTime.of(2026, 6, 20, 19, 0));

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
			.andExpect(jsonPath("$.result.lat").value(37.519))
			.andExpect(jsonPath("$.result.lng").value(127.127));
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
		jdbcTemplate.update("""
			INSERT INTO concerts (
				external_id, title, venue_name, start_at, end_at, lat, lng, source, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			externalId,
			title,
			venueName,
			startAt,
			startAt.plusHours(2),
			new BigDecimal("37.5190000"),
			new BigDecimal("127.1270000"),
			"SEED",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM concerts WHERE external_id = ?", Long.class, externalId);
	}

	private User saveCompletedUser(String nickname) {
		User user = User.createKakao("kakao-" + nickname, nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile(nickname, AgeRange.TWENTIES, UserGender.FEMALE, true, true);
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
