package com.buddyduck.buddyduck.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.domain.auth.kakao.KakaoAuthClient;
import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;
import com.buddyduck.buddyduck.domain.concert.kopis.KopisConcertClient;
import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import com.buddyduck.buddyduck.domain.place.kakao.KakaoLocalClient;
import com.buddyduck.buddyduck.domain.place.kakao.KakaoLocalPlaceCandidate;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("스케줄링 이전 사용자 API 플로우")
class PreScheduleApiFlowTest {

	private static final String REDIRECT_URI = "http://localhost:5173/oauth/kakao/callback";

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
	private KakaoAuthClient kakaoAuthClient;

	@MockBean
	private KakaoLocalClient kakaoLocalClient;

	@MockBean
	private KopisConcertClient kopisConcertClient;

	private Long concertId;

	@BeforeEach
	void setUp() {
		deleteAll();
		concertId = insertConcert(
			"pre-schedule-flow-concert",
			"Stadium Tour - Night 1",
			"KSPO Dome",
			LocalDateTime.of(2026, 7, 5, 19, 0)
		);
	}

	@AfterEach
	void tearDown() {
		deleteAll();
	}

	@Test
	@DisplayName("OAuth 신규 가입자는 프로필 완료 전 공개/프로필 API만 쓰고, 프로필 완료 후 보호 API에 진입한다")
	void oauth_가입부터_프로필_완료_가드까지_검증한다() throws Exception {
		given(kakaoAuthClient.getUserInfo("new-user-code", REDIRECT_URI))
			.willReturn(new KakaoUserInfo("kakao-new-user", "duck_new", AgeRange.TWENTIES, UserGender.FEMALE));

		MvcResult loginResult = mockMvc.perform(post("/api/auth/kakao/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"code", "new-user-code",
					"redirectUri", REDIRECT_URI
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.isNewUser").value(true))
			.andExpect(jsonPath("$.result.profileCompleted").value(false))
			.andReturn();
		String accessToken = result(loginResult).path("accessToken").asText();

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.profileCompleted").value(false));

		mockMvc.perform(get("/api/concerts")
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].posterUrl").value("https://www.kopis.or.kr/upload/pfmPoster/PF_TEST.gif"))
			.andExpect(jsonPath("$.result.items[0].timeGuidance").value("화요일 ~ 금요일(20:00), 토요일(16:00,19:00)"));

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("tags", List.of("GOODS_BUYING")))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED_PROFILE_INFO"));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"nickname", "duck_new",
					"ageRange", "TWENTIES",
					"gender", "FEMALE"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.profileCompleted").value(true));

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("tags", List.of("GOODS_BUYING")))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tags[0]").value("GOODS_BUYING"));
	}

	@Test
	@DisplayName("공연 찾기, 장소 검색, 관심 태그 저장, 방 생성까지 한 화면 흐름으로 연결된다")
	void 공연_장소_관심태그_방생성_흐름을_검증한다() throws Exception {
		User host = saveCompletedUser("host_duck");
		User applicant = saveCompletedUser("join_duck");
		String hostToken = bearer(host);
		String applicantToken = bearer(applicant);

		given(kakaoLocalClient.isEnabled()).willReturn(true);
		given(kakaoLocalClient.searchKeyword("잠실 카페"))
			.willReturn(List.of(new KakaoLocalPlaceCandidate(
				PlaceSource.KAKAO_KEYWORD,
				"kakao-cafe-1",
				"잠실 카페 mood",
				"서울 송파구 올림픽로 300",
				new BigDecimal("37.5150000"),
				new BigDecimal("127.1020000")
			)));
		given(kakaoLocalClient.searchAddress("서울 송파구 올림픽로 424"))
			.willReturn(List.of(new KakaoLocalPlaceCandidate(
				PlaceSource.KAKAO_ADDRESS,
				null,
				"서울 송파구 올림픽로 424",
				"서울 송파구 올림픽로 424",
				new BigDecimal("37.5190000"),
				new BigDecimal("127.1270000")
			)));

		mockMvc.perform(get("/api/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("UP"));

		mockMvc.perform(get("/api/concerts")
				.param("keyword", "Stadium")
				.param("from", "2026-07-01")
				.param("to", "2026-07-31")
				.param("region", "서울")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].id").value(concertId))
			.andExpect(jsonPath("$.result.items[0].endAt").value("2026-07-05T21:30:00+09:00"))
			.andExpect(jsonPath("$.result.items[0].posterUrl").value("https://www.kopis.or.kr/upload/pfmPoster/PF_TEST.gif"))
			.andExpect(jsonPath("$.result.items[0].timeGuidance").value("화요일 ~ 금요일(20:00), 토요일(16:00,19:00)"));
		verifyNoInteractions(kopisConcertClient);

		mockMvc.perform(get("/api/places/search")
				.header(HttpHeaders.AUTHORIZATION, applicantToken)
				.param("keyword", "잠실 카페"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].providerPlaceId").value("kakao-cafe-1"))
			.andExpect(jsonPath("$.result.items[0].name").value("잠실 카페 mood"));

		MvcResult meetingPlaceResult = mockMvc.perform(post("/api/places")
				.header(HttpHeaders.AUTHORIZATION, hostToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"provider", "KAKAO_KEYWORD",
					"providerPlaceId", "kakao-station-1",
					"name", "잠실역 5번 출구",
					"address", "서울 송파구 잠실동",
					"lat", 37.513,
					"lng", 127.100
				))))
			.andExpect(status().isOk())
			.andReturn();
		long meetingPlaceId = result(meetingPlaceResult).path("placeId").asLong();

		MvcResult duplicatedMeetingPlaceResult = mockMvc.perform(post("/api/places")
				.header(HttpHeaders.AUTHORIZATION, hostToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"provider", "KAKAO_KEYWORD",
					"providerPlaceId", "kakao-station-1",
					"name", "잠실역 5번 출구",
					"address", "서울 송파구 잠실동",
					"lat", 37.513,
					"lng", 127.100
				))))
			.andExpect(status().isOk())
			.andReturn();
		assertThat(result(duplicatedMeetingPlaceResult).path("placeId").asLong()).isEqualTo(meetingPlaceId);

		mockMvc.perform(get("/api/places/geocode")
				.header(HttpHeaders.AUTHORIZATION, hostToken)
				.param("address", "서울 송파구 올림픽로 424"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].lat").value(37.519))
			.andExpect(jsonPath("$.result.items[0].lng").value(127.127));

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"tags", List.of("GOODS_BUYING", "CAFE_VISIT")
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tags[0]").value("GOODS_BUYING"))
			.andExpect(jsonPath("$.result.tags[1]").value("CAFE_VISIT"));

		mockMvc.perform(put("/api/concerts/{concertId}/interest-tags/me", concertId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"tags", List.of("GOODS_BUYING", "CAFE_VISIT")
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.tags.length()").value(2));
		assertInterestTagCount(applicant.getId(), 2);

		MvcResult roomResult = mockMvc.perform(post("/api/rooms")
				.header(HttpHeaders.AUTHORIZATION, hostToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRoomPayload(4))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomId").isNumber())
			.andExpect(jsonPath("$.result.scheduleId").isNumber())
			.andReturn();
		long roomId = result(roomResult).path("roomId").asLong();

		mockMvc.perform(get("/api/concerts/{concertId}/rooms", concertId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].id").value(roomId))
			.andExpect(jsonPath("$.result.items[0].roomTags.length()").value(2))
			.andExpect(jsonPath("$.result.items[0].matchCount").value(2))
			.andExpect(jsonPath("$.result.items[0].isFull").value(false));
	}

	@Test
	@DisplayName("입장 신청, 방장 승인, 내 방 목록, 오픈채팅 조회까지 방 참여 흐름이 이어진다")
	void 방_참여_승인_오픈채팅_흐름을_검증한다() throws Exception {
		User host = saveCompletedUser("host_duck");
		User applicant = saveCompletedUser("join_duck");
		User visitor = saveCompletedUser("visit_duck");
		String hostToken = bearer(host);
		String applicantToken = bearer(applicant);
		String visitorToken = bearer(visitor);
		saveInterestTags(applicant.getId(), List.of("GOODS_BUYING", "CAFE_VISIT"));

		long roomId = createRoom(hostToken, 4);

		mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.viewerRole").value("VISITOR"))
			.andExpect(jsonPath("$.result.viewerJoinStatus").value("NOT_REQUESTED"))
			.andExpect(jsonPath("$.result.permissions.canRequestJoin").value(true))
			.andExpect(jsonPath("$.result.permissions.canViewOpenChat").value(false));

		MvcResult joinResult = mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "같이 이동하고 싶어요"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("PENDING"))
			.andReturn();
		long joinRequestId = result(joinResult).path("joinRequestId").asLong();

		mockMvc.perform(get("/api/rooms/{roomId}/join-requests/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("PENDING"));

		mockMvc.perform(get("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, hostToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].requestId").value(joinRequestId))
			.andExpect(jsonPath("$.result.items[0].nickname").value("join_duck"))
			.andExpect(jsonPath("$.result.items[0].matchedTags[0]").value("GOODS_BUYING"))
			.andExpect(jsonPath("$.result.items[0].matchedTags[1]").value("CAFE_VISIT"));

		mockMvc.perform(get("/api/rooms/{roomId}/open-chat", roomId)
				.header(HttpHeaders.AUTHORIZATION, visitorToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("COMMON403"));

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests/{requestId}/approve", roomId, joinRequestId)
				.header(HttpHeaders.AUTHORIZATION, hostToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("APPROVED"));

		mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.viewerRole").value("MEMBER"))
			.andExpect(jsonPath("$.result.permissions.canViewOpenChat").value(true));

		mockMvc.perform(get("/api/rooms/{roomId}/open-chat", roomId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.openChatUrl").value("https://open.kakao.com/o/test"))
			.andExpect(jsonPath("$.result.openChatPassword").value("1234"));

		mockMvc.perform(get("/api/me/rooms")
				.header(HttpHeaders.AUTHORIZATION, hostToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].viewerRole").value("HOST"));

		mockMvc.perform(get("/api/me/rooms")
				.header(HttpHeaders.AUTHORIZATION, applicantToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].viewerRole").value("MEMBER"))
			.andExpect(jsonPath("$.result.items[0].viewerJoinStatus").value("APPROVED"));
	}

	@Test
	@DisplayName("중복 신청, 방장 자기 신청, 정원 초과는 서버 500이 아니라 명시적인 409로 막는다")
	void 방_신청_경계_조건은_명시적인_409로_응답한다() throws Exception {
		User host = saveCompletedUser("host_duck");
		User applicant = saveCompletedUser("join_duck");
		User visitor = saveCompletedUser("visit_duck");
		String hostToken = bearer(host);
		String applicantToken = bearer(applicant);
		String visitorToken = bearer(visitor);
		long roomId = createRoom(hostToken, 2);

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, hostToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "방장 자기 신청"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("JOIN01"));

		MvcResult joinResult = mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "첫 신청"))))
			.andExpect(status().isOk())
			.andReturn();
		long joinRequestId = result(joinResult).path("joinRequestId").asLong();

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, applicantToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "중복 신청"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("JOIN01"));

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests/{requestId}/approve", roomId, joinRequestId)
				.header(HttpHeaders.AUTHORIZATION, hostToken))
			.andExpect(status().isOk());

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, visitorToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "정원 초과 신청"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ROOM01"));

		mockMvc.perform(get("/api/concerts/{concertId}/rooms", concertId)
				.header(HttpHeaders.AUTHORIZATION, visitorToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].isFull").value(true))
			.andExpect(jsonPath("$.result.items[0].status").value("FULL"));
	}

	private long createRoom(String bearerToken, int maxMembers) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/rooms")
				.header(HttpHeaders.AUTHORIZATION, bearerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRoomPayload(maxMembers))))
			.andExpect(status().isOk())
			.andReturn();
		return result(result).path("roomId").asLong();
	}

	private Map<String, Object> createRoomPayload(int maxMembers) {
		return Map.of(
			"concertId", concertId,
			"title", "굿즈 구매 동선 같이 맞출 분",
			"description", "굿즈 구매 동선을 같이 맞추고 카페도 들러요.",
			"maxMembers", maxMembers,
			"roomTags", List.of("GOODS_BUYING", "CAFE_VISIT", "CAFE_VISIT"),
			"meetingAt", "2026-07-05T14:00:00+09:00",
			"meetingPlace", Map.of(
				"provider", "KAKAO_KEYWORD",
				"providerPlaceId", "kakao-station-1",
				"name", "잠실역 5번 출구",
				"address", "서울 송파구 잠실동",
				"lat", 37.513,
				"lng", 127.100
			),
			"eventPlace", Map.of(
				"provider", "CONCERT",
				"name", "KSPO Dome",
				"address", "서울 송파구 올림픽로 424",
				"lat", 37.519,
				"lng", 127.127
			),
			"openChatUrl", "https://open.kakao.com/o/test",
			"openChatPassword", "1234"
		);
	}

	private JsonNode result(MvcResult mvcResult) throws Exception {
		return objectMapper.readTree(mvcResult.getResponse().getContentAsString()).path("result");
	}

	private User saveCompletedUser(String nickname) {
		User user = User.createKakao("kakao-" + nickname, nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile(nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		return userRepository.save(user);
	}

	private void saveInterestTags(Long userId, List<String> tags) {
		for (String tag : tags) {
			jdbcTemplate.update(
				"INSERT INTO concert_interest_tags (user_id, concert_id, tag) VALUES (?, ?, ?)",
				userId,
				concertId,
				tag
			);
		}
	}

	private void assertInterestTagCount(Long userId, int expectedCount) {
		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM concert_interest_tags WHERE user_id = ? AND concert_id = ?",
			Integer.class,
			userId,
			concertId
		);
		assertThat(count).isEqualTo(expectedCount);
	}

	private Long insertConcert(String externalId, String title, String venueName, LocalDateTime startAt) {
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
			startAt.plusHours(2).plusMinutes(30),
			new BigDecimal("37.5190000"),
			new BigDecimal("127.1270000"),
			"KOPIS",
			"https://www.kopis.or.kr/upload/pfmPoster/PF_TEST.gif",
			"서울",
			"K-POP",
			"화요일 ~ 금요일(20:00), 토요일(16:00,19:00)",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM concerts WHERE source = ? AND external_id = ?",
			Long.class,
			"KOPIS",
			externalId
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

	private String bearer(User user) {
		return bearer(jwtTokenProvider.createAccessToken(new AuthUser(user.getId())));
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
