package com.buddyduck.buddyduck.domain.room.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.security.AuthUser;
import com.buddyduck.buddyduck.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest {

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
	private User applicant;
	private User visitor;
	private Long concertId;
	private Long meetingPlaceId;
	private Long eventPlaceId;

	@BeforeEach
	void setUp() {
		deleteAll();
		host = saveCompletedUser("host_duck", true, true);
		applicant = saveCompletedUser("join_duck", true, true);
		visitor = saveCompletedUser("visit_duck", true, true);
		concertId = insertConcert();
		meetingPlaceId = insertPlace("잠실역 5번 출구", "서울 송파구 잠실동");
		eventPlaceId = insertPlace("KSPO Dome", "서울 송파구 올림픽로 424");
		insertInterestTag(applicant.getId(), concertId, "GOODS_BUYING");
		insertInterestTag(applicant.getId(), concertId, "CAFE_VISIT");
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	void 방을_생성하면_host_member와_기본_schedule을_함께_생성한다() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/rooms")
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRoomPayload())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomId").isNumber())
			.andExpect(jsonPath("$.result.scheduleId").isNumber())
			.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
		long roomId = response.path("result").path("roomId").asLong();

		Integer hostMemberCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = ? AND user_id = ? AND role = 'HOST'",
			Integer.class,
			roomId,
			host.getId()
		);
		assertThat(hostMemberCount).isEqualTo(1);
	}

	@Test
	void 공연별_방_목록은_관심_태그_matchCount를_계산한다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		insertRoomTag(roomId, "MEAL_TOGETHER");

		mockMvc.perform(get("/api/concerts/{concertId}/rooms", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].id").value(roomId))
			.andExpect(jsonPath("$.result.items[0].title").value("굿즈 같이 갈 분"))
			.andExpect(jsonPath("$.result.items[0].hostNickname").value("host_duck"))
			.andExpect(jsonPath("$.result.items[0].memberCount").value(1))
			.andExpect(jsonPath("$.result.items[0].maxMembers").value(4))
			.andExpect(jsonPath("$.result.items[0].meetingPlaceName").value("잠실역 5번 출구"))
			.andExpect(jsonPath("$.result.items[0].matchCount").value(1));
	}

	@Test
	void 방_상세는_viewer_state와_permissions를_반환한다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.id").value(roomId))
			.andExpect(jsonPath("$.result.viewerRole").value("VISITOR"))
			.andExpect(jsonPath("$.result.viewerJoinStatus").value("NOT_REQUESTED"))
			.andExpect(jsonPath("$.result.permissions.canRequestJoin").value(true))
			.andExpect(jsonPath("$.result.permissions.canViewOpenChat").value(false))
			.andExpect(jsonPath("$.result.pendingRequestCount").value(0));
	}

	@Test
	void 입장_신청_후_방장이_승인하면_멤버가_되고_오픈채팅을_조회할_수_있다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		MvcResult joinResult = mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "같이 이동하고 싶어요"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("PENDING"))
			.andReturn();
		long requestId = objectMapper.readTree(joinResult.getResponse().getContentAsString())
			.path("result")
			.path("joinRequestId")
			.asLong();

		mockMvc.perform(get("/api/rooms/{roomId}/join-requests/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("PENDING"))
			.andExpect(jsonPath("$.result.message").value("같이 이동하고 싶어요"));

		mockMvc.perform(get("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].requestId").value(requestId))
			.andExpect(jsonPath("$.result.items[0].nickname").value("join_duck"))
			.andExpect(jsonPath("$.result.items[0].matchedTags[0]").value("GOODS_BUYING"));

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests/{requestId}/approve", roomId, requestId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.requestId").value(requestId))
			.andExpect(jsonPath("$.result.status").value("APPROVED"))
			.andExpect(jsonPath("$.result.memberId").isNumber());

		mockMvc.perform(get("/api/rooms/{roomId}/open-chat", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.openChatUrl").value("https://open.kakao.com/o/test"))
			.andExpect(jsonPath("$.result.openChatPassword").value("1234"));
	}

	@Test
	void 방장이_아니면_신청_목록을_조회할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(get("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("COMMON403"));
	}

	@Test
	void 중복_입장_신청은_409를_응답한다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "처음 신청"))))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "다시 신청"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("JOIN01"));
	}

	@Test
	void 입장_신청을_거절한다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		Long requestId = insertJoinRequest(roomId, applicant.getId(), "같이 이동하고 싶어요");

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests/{requestId}/reject", roomId, requestId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.requestId").value(requestId))
			.andExpect(jsonPath("$.result.status").value("REJECTED"));
	}

	@Test
	void 내_방_목록을_조회한다() throws Exception {
		Long hostedRoomId = insertRoom(host, "내가 만든 방", 4);
		Long pendingRoomId = insertRoom(visitor, "신청한 방", 4);
		insertJoinRequest(pendingRoomId, host.getId(), "신청 중");

		mockMvc.perform(get("/api/me/rooms")
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].roomId").value(hostedRoomId))
			.andExpect(jsonPath("$.result.items[0].viewerRole").value("HOST"))
			.andExpect(jsonPath("$.result.items[0].viewerJoinStatus").value("APPROVED"))
			.andExpect(jsonPath("$.result.items[1].roomId").value(pendingRoomId))
			.andExpect(jsonPath("$.result.items[1].viewerJoinStatus").value("PENDING"));
	}

	@Test
	void 승인되지_않은_사용자는_오픈채팅을_조회할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(get("/api/rooms/{roomId}/open-chat", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("COMMON403"));
	}

	@Test
	void 데모_방_seed를_생성한다() throws Exception {
		mockMvc.perform(post("/api/dev/seed/demo-room"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomId").isNumber())
			.andExpect(jsonPath("$.result.scheduleId").isNumber());
	}

	private Map<String, Object> createRoomPayload() {
		return Map.of(
			"concertId", concertId,
			"title", "굿즈 구매 동선 같이 맞출 분",
			"description", "굿즈 구매 동선을 같이 맞추고 카페도 들러요.",
			"maxMembers", 4,
			"roomTags", List.of("GOODS_BUYING", "CAFE_VISIT"),
			"meetingAt", "2026-06-15T14:00:00+09:00",
			"meetingPlace", Map.of(
				"name", "잠실역 5번 출구",
				"address", "서울 송파구 잠실동",
				"lat", 37.513,
				"lng", 127.100,
				"provider", "KAKAO_ADDRESS"
			),
			"eventPlace", Map.of(
				"name", "KSPO Dome",
				"address", "서울 송파구 올림픽로 424",
				"lat", 37.519,
				"lng", 127.127,
				"provider", "CONCERT"
			),
			"openChatUrl", "https://open.kakao.com/o/test",
			"openChatPassword", "1234"
		);
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

	private User saveCompletedUser(String nickname, boolean ageVisible, boolean genderVisible) {
		User user = User.createKakao("kakao-" + nickname, nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile(nickname, AgeRange.TWENTIES, UserGender.FEMALE, ageVisible, genderVisible);
		return userRepository.save(user);
	}

	private Long insertConcert() {
		jdbcTemplate.update("""
			INSERT INTO concerts (
				external_id, title, venue_name, start_at, end_at, lat, lng, source, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			"room-test-concert",
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
		return jdbcTemplate.queryForObject("SELECT id FROM concerts WHERE external_id = ?", Long.class, "room-test-concert");
	}

	private Long insertPlace(String name, String address) {
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
			new BigDecimal("37.5150000"),
			new BigDecimal("127.1020000"),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM places WHERE provider_place_id = ?", Long.class, providerPlaceId);
	}

	private Long insertRoom(User hostUser, String title, int maxMembers) {
		String openChatUrl = "https://open.kakao.com/o/test";
		jdbcTemplate.update("""
			INSERT INTO rooms (
				concert_id, host_user_id, title, description, max_members, meeting_at,
				meeting_place_id, event_place_id, open_chat_url, open_chat_password, status,
				created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			concertId,
			hostUser.getId(),
			title,
			"설명",
			maxMembers,
			LocalDateTime.of(2026, 6, 15, 14, 0),
			meetingPlaceId,
			eventPlaceId,
			openChatUrl,
			"1234",
			"OPEN",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		Long roomId = jdbcTemplate.queryForObject(
			"SELECT id FROM rooms WHERE title = ? ORDER BY id DESC LIMIT 1",
			Long.class,
			title
		);
		jdbcTemplate.update(
			"INSERT INTO room_members (room_id, user_id, role, joined_at) VALUES (?, ?, 'HOST', ?)",
			roomId,
			hostUser.getId(),
			LocalDateTime.now()
		);
		insertRoomTag(roomId, "GOODS_BUYING");
		return roomId;
	}

	private void insertRoomTag(Long roomId, String tag) {
		jdbcTemplate.update("INSERT INTO room_tags (room_id, tag) VALUES (?, ?)", roomId, tag);
	}

	private void insertInterestTag(Long userId, Long concertId, String tag) {
		jdbcTemplate.update(
			"INSERT INTO concert_interest_tags (user_id, concert_id, tag) VALUES (?, ?, ?)",
			userId,
			concertId,
			tag
		);
	}

	private Long insertJoinRequest(Long roomId, Long userId, String message) {
		jdbcTemplate.update("""
			INSERT INTO join_requests (
				room_id, user_id, message, status, created_at, updated_at
			) VALUES (?, ?, ?, 'PENDING', ?, ?)
			""",
			roomId,
			userId,
			message,
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM join_requests WHERE room_id = ? AND user_id = ?",
			Long.class,
			roomId,
			userId
		);
	}

	private String bearer(User user) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));
	}
}
