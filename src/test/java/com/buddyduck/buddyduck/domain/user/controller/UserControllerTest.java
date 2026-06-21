package com.buddyduck.buddyduck.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.security.AuthUser;
import com.buddyduck.buddyduck.global.security.JwtTokenProvider;
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
class UserControllerTest {

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
		deleteAll();
	}

	@Test
	void 토큰이_없으면_me_요청에_401을_응답한다() throws Exception {
		mockMvc.perform(get("/api/users/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON401"));
	}

	@Test
	void 잘못된_토큰이면_me_요청에_401을_응답한다() throws Exception {
		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON401"));
	}

	@Test
	void 유효한_JWT가_있으면_내_프로필을_조회한다() throws Exception {
		User user = userRepository.save(User.createKakao(
			"12345",
			"duck_fan",
			AgeRange.TWENTIES,
			UserGender.FEMALE
		));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.id").value(user.getId()))
				.andExpect(jsonPath("$.result.nickname").value("duck_fan"))
				.andExpect(jsonPath("$.result.ageRange").value("TWENTIES"))
				.andExpect(jsonPath("$.result.gender").value("FEMALE"))
				.andExpect(jsonPath("$.result.profileCompleted").value(false))
				.andExpect(jsonPath("$.result.avatarColor").value("#FACC15"))
				.andExpect(jsonPath("$.result.participatingRoomCount").value(0))
				.andExpect(jsonPath("$.result.pendingRoomCount").value(0));
	}

	@Test
	void 내_프로필은_참여중인_방과_대기중인_방_수를_함께_반환한다() throws Exception {
		User user = userRepository.save(User.createKakao(
			"12345",
			"duck_fan",
			AgeRange.TWENTIES,
			UserGender.FEMALE
		));
		User host = userRepository.save(User.createKakao(
			"67890",
			"host_duck",
			AgeRange.TWENTIES,
			UserGender.FEMALE
		));
		Long concertId = insertConcert();
		Long meetingPlaceId = insertPlace("잠실역 5번 출구");
		Long eventPlaceId = insertPlace("KSPO Dome");
		Long joinedRoomId = insertRoom(host.getId(), concertId, meetingPlaceId, eventPlaceId, "참여중인 방");
		Long pendingRoomId = insertRoom(host.getId(), concertId, meetingPlaceId, eventPlaceId, "대기중인 방");
		insertRoomMember(joinedRoomId, user.getId(), "MEMBER");
		insertJoinRequest(pendingRoomId, user.getId());
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.participatingRoomCount").value(1))
			.andExpect(jsonPath("$.result.pendingRoomCount").value(1));
	}

	@Test
	void 추가_프로필을_저장하면_회원가입이_완료된다() throws Exception {
		User user = userRepository.save(User.createKakao(
				"12345",
				"duck_fan",
				null,
				null
			));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of(
						"nickname", "moon_armies",
						"ageRange", "TWENTIES",
						"gender", "FEMALE"
					))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value("COMMON200"))
				.andExpect(jsonPath("$.result.nickname").value("moon_armies"))
				.andExpect(jsonPath("$.result.ageRange").value("TWENTIES"))
				.andExpect(jsonPath("$.result.gender").value("FEMALE"))
				.andExpect(jsonPath("$.result.profileCompleted").value(true));
	}

	@Test
	void 추가_프로필에서_연령대를_누락하면_400을_응답한다() throws Exception {
		User user = userRepository.save(User.createKakao(
			"12345",
			"duck_fan",
			null,
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"nickname", "moon_armies",
					"gender", "FEMALE"
				))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	@Test
	void 추가_프로필에서_성별을_누락하면_400을_응답한다() throws Exception {
		User user = userRepository.save(User.createKakao(
			"12345",
			"duck_fan",
			null,
			null
		));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"nickname", "moon_armies",
					"ageRange", "TWENTIES"
				))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	private void deleteAll() {
		jdbcTemplate.update("DELETE FROM route_segments");
		jdbcTemplate.update("DELETE FROM schedule_slots");
		jdbcTemplate.update("DELETE FROM schedules");
		jdbcTemplate.update("DELETE FROM join_requests");
		jdbcTemplate.update("DELETE FROM room_tags");
		jdbcTemplate.update("DELETE FROM room_members");
		jdbcTemplate.update("DELETE FROM rooms");
		jdbcTemplate.update("DELETE FROM concert_interest_tags");
		jdbcTemplate.update("DELETE FROM places");
		jdbcTemplate.update("DELETE FROM concerts");
		userRepository.deleteAll();
	}

	private Long insertConcert() {
		jdbcTemplate.update("""
			INSERT INTO concerts (
				external_id, title, venue_name, start_at, end_at, lat, lng, source, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			"profile-test-concert",
			"AURORA LIVE",
			"KSPO Dome",
			LocalDateTime.of(2026, 7, 5, 19, 0),
			LocalDateTime.of(2026, 7, 5, 21, 30),
			new BigDecimal("37.5190000"),
			new BigDecimal("127.1270000"),
			"SEED",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM concerts WHERE external_id = ?", Long.class, "profile-test-concert");
	}

	private Long insertPlace(String name) {
		String providerPlaceId = name + "-" + System.nanoTime();
		jdbcTemplate.update("""
			INSERT INTO places (
				provider, provider_place_id, name, address, lat, lng, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""",
			"KAKAO_ADDRESS",
			providerPlaceId,
			name,
			"서울 송파구",
			new BigDecimal("37.5150000"),
			new BigDecimal("127.1020000"),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM places WHERE provider_place_id = ?", Long.class, providerPlaceId);
	}

	private Long insertRoom(Long hostUserId, Long concertId, Long meetingPlaceId, Long eventPlaceId, String title) {
		jdbcTemplate.update("""
			INSERT INTO rooms (
				concert_id, host_user_id, title, description, max_members, meeting_at,
				meeting_place_id, event_place_id, open_chat_url, open_chat_password, status,
				created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			concertId,
			hostUserId,
			title,
			"설명",
			4,
			LocalDateTime.of(2026, 7, 5, 14, 0),
			meetingPlaceId,
			eventPlaceId,
			"https://open.kakao.com/o/test",
			"1234",
			"OPEN",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM rooms WHERE title = ? ORDER BY id DESC LIMIT 1",
			Long.class,
			title
		);
	}

	private void insertRoomMember(Long roomId, Long userId, String role) {
		jdbcTemplate.update(
			"INSERT INTO room_members (room_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)",
			roomId,
			userId,
			role,
			LocalDateTime.now()
		);
	}

	private void insertJoinRequest(Long roomId, Long userId) {
		jdbcTemplate.update("""
			INSERT INTO join_requests (
				room_id, user_id, message, status, created_at, updated_at
			) VALUES (?, ?, ?, 'PENDING', ?, ?)
			""",
			roomId,
			userId,
			"신청 중",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
	}
}
