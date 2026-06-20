package com.buddyduck.buddyduck.domain.schedule.controller;

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
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
class ScheduleControllerTest {

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

	private User host;
	private User member;
	private User visitor;
	private Long roomId;
	private Long scheduleId;
	private Long meetingPlaceId;
	private Long cafePlaceId;

	@BeforeEach
	void setUp() {
		deleteAll();
		host = saveCompletedUser("host_duck");
		member = saveCompletedUser("member_duck");
		visitor = saveCompletedUser("visitor_duck");
		Long concertId = insertConcert();
		meetingPlaceId = insertPlace("잠실역 5번 출구", "서울 송파구 잠실동", "37.5130000", "127.1000000");
		Long eventPlaceId = insertPlace("KSPO Dome", "서울 송파구 올림픽로 424", "37.5190000", "127.1270000");
		cafePlaceId = insertPlace("잠실 카페 무드", "서울 송파구 올림픽로 300", "37.5150000", "127.1020000");
		roomId = insertRoom(concertId, eventPlaceId);
		scheduleId = insertSchedule(roomId);
		insertMember(roomId, host.getId(), "HOST");
		insertMember(roomId, member.getId(), "MEMBER");
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	void 타임라인을_조회한다() throws Exception {
		mockMvc.perform(get("/api/rooms/{roomId}/timeline", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(member)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.room.id").value(roomId))
			.andExpect(jsonPath("$.result.room.title").value("굿즈 같이 갈 분"))
			.andExpect(jsonPath("$.result.schedule.id").value(scheduleId))
			.andExpect(jsonPath("$.result.schedule.arrivalBufferMinutes").value(30))
			.andExpect(jsonPath("$.result.schedule.timezone").value("Asia/Seoul"))
			.andExpect(jsonPath("$.result.slots.length()").value(0))
			.andExpect(jsonPath("$.result.routeSegments.length()").value(0));
	}

	@Test
	void 승인되지_않은_사용자는_타임라인을_조회할_수_없다() throws Exception {
		mockMvc.perform(get("/api/rooms/{roomId}/timeline", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("COMMON403"));
	}

	@Test
	void draft_preview는_DB에_저장하지_않고_계산_결과만_반환한다() throws Exception {
		mockMvc.perform(post("/api/schedules/{scheduleId}/draft/recalculate", scheduleId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(draftPayload())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.fitStatus").value("OK"))
			.andExpect(jsonPath("$.result.overrunMinutes").value(0))
			.andExpect(jsonPath("$.result.slots[0].clientId").value("slot-meeting"))
			.andExpect(jsonPath("$.result.routeSegments[0].mode").value("WALK"))
			.andExpect(jsonPath("$.result.routeSegments[0].distanceMeters").isNumber())
			.andExpect(jsonPath("$.result.routeSegments[0].provider").value("FALLBACK_STRAIGHT_LINE"))
			.andExpect(jsonPath("$.result.routeSegments[0].manuallyAdjusted").value(false))
			.andExpect(jsonPath("$.result.routeSegments[0].taxiFareWon").doesNotExist());

		Integer slotCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM schedule_slots WHERE schedule_id = ?",
			Integer.class,
			scheduleId
		);
		Integer routeCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM route_segments WHERE schedule_id = ?",
			Integer.class,
			scheduleId
		);
		assertThat(slotCount).isEqualTo(0);
		assertThat(routeCount).isEqualTo(0);
	}

	@Test
	void draft_preview는_없는_slot_참조를_400으로_응답한다() throws Exception {
		ObjectNode payload = objectMapper.valueToTree(draftPayload());
		ArrayNode routeSegments = (ArrayNode) payload.path("routeSegments");
		((ObjectNode) routeSegments.get(0)).put("toClientId", "missing-slot");

		mockMvc.perform(post("/api/schedules/{scheduleId}/draft/recalculate", scheduleId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void draft_commit은_슬롯과_이동구간을_DB에_저장한다() throws Exception {
		mockMvc.perform(put("/api/schedules/{scheduleId}/draft/commit", scheduleId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(draftPayload())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.room.id").value(roomId))
			.andExpect(jsonPath("$.result.schedule.id").value(scheduleId))
			.andExpect(jsonPath("$.result.slots[0].title").value("잠실역 5번 출구"))
			.andExpect(jsonPath("$.result.slots[1].startAt").value("2026-06-15T14:16:00+09:00"))
			.andExpect(jsonPath("$.result.routeSegments[0].mode").value("WALK"))
			.andExpect(jsonPath("$.result.routeSegments[0].durationMinutes").value(6))
			.andExpect(jsonPath("$.result.routeSegments[0].distanceMeters").isNumber())
			.andExpect(jsonPath("$.result.routeSegments[0].provider").value("FALLBACK_STRAIGHT_LINE"))
			.andExpect(jsonPath("$.result.routeSegments[0].manuallyAdjusted").value(false));

		Integer slotCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM schedule_slots WHERE schedule_id = ?",
			Integer.class,
			scheduleId
		);
		Integer routeCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM route_segments WHERE schedule_id = ?",
			Integer.class,
			scheduleId
		);
		assertThat(slotCount).isEqualTo(2);
		assertThat(routeCount).isEqualTo(1);
	}

	@Test
	void 지도_조회는_핀과_bounds를_반환한다() throws Exception {
		commitDraft();

		mockMvc.perform(get("/api/rooms/{roomId}/map", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(member)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.slots[0].title").value("잠실역 5번 출구"))
			.andExpect(jsonPath("$.result.routeSegments[0].mode").value("WALK"))
			.andExpect(jsonPath("$.result.routeSegments[0].distanceMeters").isNumber())
			.andExpect(jsonPath("$.result.routeSegments[0].provider").value("FALLBACK_STRAIGHT_LINE"))
			.andExpect(jsonPath("$.result.mapBounds.southWest.lat").value(37.513))
			.andExpect(jsonPath("$.result.mapBounds.northEast.lng").value(127.102));
	}

	@Test
	void 수동_조정된_이동구간은_입력된_시간을_그대로_사용한다() throws Exception {
		ObjectNode payload = objectMapper.valueToTree(draftPayload());
		ArrayNode routeSegments = (ArrayNode) payload.path("routeSegments");
		((ObjectNode) routeSegments.get(0)).put("manuallyAdjusted", true);

		mockMvc.perform(post("/api/schedules/{scheduleId}/draft/recalculate", scheduleId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.routeSegments[0].durationMinutes").value(18))
			.andExpect(jsonPath("$.result.routeSegments[0].provider").value("MANUAL"))
			.andExpect(jsonPath("$.result.routeSegments[0].manuallyAdjusted").value(true))
			.andExpect(jsonPath("$.result.routeSegments[0].distanceMeters").doesNotExist());
	}

	@Test
	void 장소가_없는_이동구간은_입력_시간을_시스템_fallback으로_사용한다() throws Exception {
		ObjectNode payload = objectMapper.valueToTree(draftPayload());
		ArrayNode slots = (ArrayNode) payload.path("slots");
		((ObjectNode) slots.get(0)).remove("placeId");

		mockMvc.perform(post("/api/schedules/{scheduleId}/draft/recalculate", scheduleId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.routeSegments[0].durationMinutes").value(18))
			.andExpect(jsonPath("$.result.routeSegments[0].provider").value("UNRESOLVED_PLACE"))
			.andExpect(jsonPath("$.result.routeSegments[0].manuallyAdjusted").value(false))
			.andExpect(jsonPath("$.result.routeSegments[0].distanceMeters").doesNotExist());
	}

	private Map<String, Object> draftPayload() {
		return Map.of(
			"arrivalBufferMinutes", 30,
			"slots", List.of(
				Map.of(
					"clientId", "slot-meeting",
					"order", 1,
					"title", "잠실역 5번 출구",
					"placeId", meetingPlaceId,
					"dwellMinutes", 10,
					"locked", false,
					"slotType", "MEETING",
					"category", "MEETING"
				),
				Map.of(
					"clientId", "slot-cafe",
					"order", 2,
					"title", "잠실 카페 무드",
					"placeId", cafePlaceId,
					"dwellMinutes", 30,
					"locked", false,
					"slotType", "PLACE",
					"category", "CAFE_VISIT"
				)
			),
			"routeSegments", List.of(
				Map.of(
					"fromClientId", "slot-meeting",
					"toClientId", "slot-cafe",
					"mode", "WALK",
					"durationMinutes", 18
				)
			)
		);
	}

	private void commitDraft() throws Exception {
		mockMvc.perform(put("/api/schedules/{scheduleId}/draft/commit", scheduleId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(draftPayload())))
			.andExpect(status().isOk());
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

	private User saveCompletedUser(String nickname) {
		User user = User.createKakao("kakao-" + nickname, nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile(nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		return userRepository.save(user);
	}

	private Long insertConcert() {
		jdbcTemplate.update("""
			INSERT INTO concerts (
				external_id, title, venue_name, start_at, end_at, lat, lng, source, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			"schedule-test-concert",
			"AURORA LIVE",
			"KSPO Dome",
			LocalDateTime.of(2026, 6, 15, 19, 0),
			LocalDateTime.of(2026, 6, 15, 21, 30),
			new BigDecimal("37.5190000"),
			new BigDecimal("127.1270000"),
			"SEED",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM concerts WHERE external_id = ?", Long.class, "schedule-test-concert");
	}

	private Long insertPlace(String name, String address, String lat, String lng) {
		String providerPlaceId = name + "-" + System.nanoTime();
		jdbcTemplate.update("""
			INSERT INTO places (
				provider, provider_place_id, name, address, lat, lng, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""",
			"KAKAO_ADDRESS",
			providerPlaceId,
			name,
			address,
			new BigDecimal(lat),
			new BigDecimal(lng),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM places WHERE provider_place_id = ?", Long.class, providerPlaceId);
	}

	private Long insertRoom(Long concertId, Long eventPlaceId) {
		jdbcTemplate.update("""
			INSERT INTO rooms (
				concert_id, host_user_id, title, description, max_members, meeting_at,
				meeting_place_id, event_place_id, open_chat_url, open_chat_password, status,
				created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			concertId,
			host.getId(),
			"굿즈 같이 갈 분",
			"설명",
			4,
			LocalDateTime.of(2026, 6, 15, 14, 0),
			meetingPlaceId,
			eventPlaceId,
			"https://open.kakao.com/o/test",
			"1234",
			"OPEN",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM rooms WHERE title = ?", Long.class, "굿즈 같이 갈 분");
	}

	private Long insertSchedule(Long roomId) {
		jdbcTemplate.update("""
			INSERT INTO schedules (
				room_id, arrival_buffer_minutes, version, created_at, updated_at
			) VALUES (?, 30, 0, ?, ?)
			""",
			roomId,
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM schedules WHERE room_id = ?", Long.class, roomId);
	}

	private void insertMember(Long roomId, Long userId, String role) {
		jdbcTemplate.update(
			"INSERT INTO room_members (room_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)",
			roomId,
			userId,
			role,
			LocalDateTime.now()
		);
	}

	private String bearer(User user) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));
	}
}
