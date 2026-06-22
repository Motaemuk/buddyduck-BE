package com.buddyduck.buddyduck.domain.room.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.domain.room.dto.RoomDateTimeFormatter;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.security.AuthUser;
import com.buddyduck.buddyduck.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
		host = saveCompletedUser("host_duck");
		applicant = saveCompletedUser("join_duck");
		visitor = saveCompletedUser("visit_duck");
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
		long scheduleId = response.path("result").path("scheduleId").asLong();

		Integer hostMemberCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = ? AND user_id = ? AND role = 'HOST'",
			Integer.class,
			roomId,
			host.getId()
		);
		assertThat(hostMemberCount).isEqualTo(1);

		Integer slotCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM schedule_slots WHERE schedule_id = ?",
			Integer.class,
			scheduleId
		);
		assertThat(slotCount).isEqualTo(2);
		assertThat(slotValue(scheduleId, 1, "slot_type", String.class)).isEqualTo("MEETING");
		assertThat(slotValue(scheduleId, 1, "category", String.class)).isEqualTo("MEETING");
		assertThat(slotValue(scheduleId, 1, "title", String.class)).isEqualTo("잠실역 5번 출구");
		assertThat(slotValue(scheduleId, 1, "place_id", Long.class)).isEqualTo(meetingPlaceId);
		assertThat(slotValue(scheduleId, 1, "dwell_minutes", Integer.class)).isZero();
		assertThat(slotValue(scheduleId, 1, "locked", Boolean.class)).isTrue();
		assertThat(slotValue(scheduleId, 2, "slot_type", String.class)).isEqualTo("CONCERT");
		assertThat(slotValue(scheduleId, 2, "category", String.class)).isEqualTo("CONCERT");
		assertThat(slotValue(scheduleId, 2, "title", String.class)).isEqualTo("AURORA LIVE");
		assertThat(slotPlaceName(scheduleId, 2)).isEqualTo("KSPO Dome");
		assertThat(slotValue(scheduleId, 2, "dwell_minutes", Integer.class)).isZero();
		assertThat(slotValue(scheduleId, 2, "locked", Boolean.class)).isTrue();

		Integer routeCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM route_segments WHERE schedule_id = ?",
			Integer.class,
			scheduleId
		);
		assertThat(routeCount).isEqualTo(1);
		Long meetingSlotId = slotValue(scheduleId, 1, "id", Long.class);
		Long concertSlotId = slotValue(scheduleId, 2, "id", Long.class);
		assertThat(routeValue(scheduleId, "from_slot_id", Long.class)).isEqualTo(meetingSlotId);
		assertThat(routeValue(scheduleId, "to_slot_id", Long.class)).isEqualTo(concertSlotId);
		assertThat(routeValue(scheduleId, "mode", String.class)).isEqualTo("WALK");
		assertThat(routeValue(scheduleId, "distance_meters", Integer.class)).isPositive();
		assertThat(routeValue(scheduleId, "duration_minutes", Integer.class)).isPositive();
		assertThat(routeValue(scheduleId, "provider", String.class)).isEqualTo("FALLBACK_STRAIGHT_LINE");
		assertThat(routeValue(scheduleId, "manually_adjusted", Boolean.class)).isFalse();
	}

	@Test
	void 방_장소_좌표가_범위를_벗어나면_400을_응답한다() throws Exception {
		ObjectNode payload = objectMapper.valueToTree(createRoomPayload());
		ObjectNode meetingPlace = (ObjectNode) payload.path("meetingPlace");
		meetingPlace.put("lat", 91.0);

		mockMvc.perform(post("/api/rooms")
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void 공연_endAt이_없어도_방_생성시_concert_anchor의_종료시간은_시작시간으로_저장된다() throws Exception {
		jdbcTemplate.update("UPDATE concerts SET end_at = NULL WHERE id = ?", concertId);

		MvcResult result = mockMvc.perform(post("/api/rooms")
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRoomPayload())))
			.andExpect(status().isOk())
			.andReturn();

		long scheduleId = objectMapper.readTree(result.getResponse().getContentAsString())
			.path("result")
			.path("scheduleId")
			.asLong();
		LocalDateTime startAt = slotValue(scheduleId, 2, "start_at", LocalDateTime.class);
		LocalDateTime endAt = slotValue(scheduleId, 2, "end_at", LocalDateTime.class);
		assertThat(endAt).isEqualTo(startAt);
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
		insertRoomTag(roomId, "CAFE_VISIT");
		insertMember(roomId, applicant.getId(), "MEMBER");
		Long scheduleId = insertSchedule(roomId);
		insertScheduleSlot(
			scheduleId,
			meetingPlaceId,
			"MEETING",
			"MEETING",
			"잠실역 5번 출구",
			1,
			LocalDateTime.of(2026, 6, 15, 14, 0),
			LocalDateTime.of(2026, 6, 15, 14, 15),
			15,
			true
		);
		insertScheduleSlot(
			scheduleId,
			eventPlaceId,
			"CONCERT",
			"CONCERT",
			"AURORA LIVE",
			2,
			LocalDateTime.of(2026, 6, 15, 19, 0),
			LocalDateTime.of(2026, 6, 15, 21, 30),
			0,
			true
		);

		MvcResult result = mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.id").value(roomId))
			.andExpect(jsonPath("$.result.title").value("굿즈 같이 갈 분"))
			.andExpect(jsonPath("$.result.description").value("설명"))
			.andExpect(jsonPath("$.result.roomStatus").value("OPEN"))
			.andExpect(jsonPath("$.result.viewerRole").value("VISITOR"))
			.andExpect(jsonPath("$.result.viewerJoinStatus").value("NOT_REQUESTED"))
			.andExpect(jsonPath("$.result.permissions.canRequestJoin").value(true))
			.andExpect(jsonPath("$.result.permissions.canViewOpenChat").value(false))
			.andExpect(jsonPath("$.result.pendingRequestCount").value(0))
			.andExpect(jsonPath("$.result.concert.id").value(concertId))
			.andExpect(jsonPath("$.result.concert.title").value("AURORA LIVE"))
			.andExpect(jsonPath("$.result.concert.startAt").value("2026-06-15T19:00:00+09:00"))
			.andExpect(jsonPath("$.result.concert.venueName").value("KSPO Dome"))
			.andExpect(jsonPath("$.result.meetingAt").value("2026-06-15T14:00:00+09:00"))
			.andExpect(jsonPath("$.result.meetingPlaceName").value("잠실역 5번 출구"))
			.andExpect(jsonPath("$.result.meetingPlaceAddress").value("서울 송파구 잠실동"))
			.andExpect(jsonPath("$.result.roomTags[0]").value("GOODS_BUYING"))
			.andExpect(jsonPath("$.result.roomTags[1]").value("CAFE_VISIT"))
			.andExpect(jsonPath("$.result.memberCount").value(2))
			.andExpect(jsonPath("$.result.maxMembers").value(4))
			.andExpect(jsonPath("$.result.schedulePreview[0].slotType").value("MEETING"))
			.andExpect(jsonPath("$.result.schedulePreview[0].placeName").value("잠실역 5번 출구"))
			.andExpect(jsonPath("$.result.schedulePreview[1].slotType").value("CONCERT"))
			.andExpect(jsonPath("$.result.schedulePreview[1].placeName").value("KSPO Dome"))
			.andReturn();

		JsonNode members = objectMapper.readTree(result.getResponse().getContentAsString())
			.path("result")
			.path("members");
		JsonNode applicantMember = findMemberItem(members, "join_duck");
		assertThat(applicantMember.path("role").asText()).isEqualTo("MEMBER");
		assertThat(applicantMember.path("ageRange").asText()).isEqualTo("TWENTIES");
		assertThat(applicantMember.path("gender").asText()).isEqualTo("FEMALE");
		assertThat(applicantMember.path("sharedInterestCount").asInt()).isEqualTo(2);
	}

	@Test
	void 방장은_방_정보를_수정할_수_있다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		Long scheduleId = insertSchedule(roomId);
		insertScheduleSlot(
			scheduleId,
			meetingPlaceId,
			"MEETING",
			"MEETING",
			"잠실역 5번 출구",
			1,
			LocalDateTime.of(2026, 6, 15, 14, 0),
			LocalDateTime.of(2026, 6, 15, 14, 0),
			0,
			true
		);
		insertScheduleSlot(
			scheduleId,
			eventPlaceId,
			"CONCERT",
			"CONCERT",
			"AURORA LIVE",
			2,
			LocalDateTime.of(2026, 6, 15, 19, 0),
			LocalDateTime.of(2026, 6, 15, 21, 30),
			0,
			true
		);
		insertScheduleSlot(
			scheduleId,
			meetingPlaceId,
			"MEETING",
			"MEETING",
			"사용자 지정 집합",
			3,
			LocalDateTime.of(2026, 6, 15, 13, 30),
			LocalDateTime.of(2026, 6, 15, 13, 30),
			0,
			false
		);

		mockMvc.perform(patch("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRoomPayload(5))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomId").value(roomId))
			.andExpect(jsonPath("$.result.status").value("OPEN"));

		mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.title").value("수정된 굿즈 동선 방"))
			.andExpect(jsonPath("$.result.description").value("수정된 설명입니다."))
			.andExpect(jsonPath("$.result.meetingAt").value("2026-06-15T15:00:00+09:00"))
			.andExpect(jsonPath("$.result.meetingPlaceName").value("잠실 종합운동장역"))
			.andExpect(jsonPath("$.result.maxMembers").value(5))
			.andExpect(jsonPath("$.result.roomTags[0]").value("MEAL_TOGETHER"))
			.andExpect(jsonPath("$.result.schedulePreview[0].placeName").value("잠실 종합운동장역"))
			.andExpect(jsonPath("$.result.schedulePreview[0].startAt").value("2026-06-15T15:00:00+09:00"));

		String customSlotTitle = jdbcTemplate.queryForObject(
			"SELECT title FROM schedule_slots WHERE schedule_id = ? AND sort_order = 3",
			String.class,
			scheduleId
		);
		Long customSlotPlaceId = jdbcTemplate.queryForObject(
			"SELECT place_id FROM schedule_slots WHERE schedule_id = ? AND sort_order = 3",
			Long.class,
			scheduleId
		);
		assertThat(customSlotTitle).isEqualTo("사용자 지정 집합");
		assertThat(customSlotPlaceId).isEqualTo(meetingPlaceId);
	}

	@Test
	void 방장이_아니면_방_정보를_수정할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(patch("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRoomPayload(5))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("COMMON403"));
	}

	@Test
	void 현재_멤버_수보다_작게_정원을_수정할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		insertMember(roomId, applicant.getId(), "MEMBER");

		mockMvc.perform(patch("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRoomPayload(1))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	@Test
	void 방_태그에_null이_있으면_방_정보를_수정할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		String invalidPayload = """
			{
			  "title": "수정된 굿즈 동선 방",
			  "description": "수정된 설명입니다.",
			  "maxMembers": 5,
			  "roomTags": [null],
			  "meetingAt": "2026-06-15T15:00:00+09:00",
			  "meetingPlace": {
			    "name": "잠실 종합운동장역",
			    "address": "서울 송파구 올림픽로 지하 23",
			    "lat": 37.510,
			    "lng": 127.073,
			    "provider": "KAKAO_ADDRESS"
			  },
			  "eventPlace": {
			    "name": "KSPO Dome",
			    "address": "서울 송파구 올림픽로 424",
			    "lat": 37.519,
			    "lng": 127.127,
			    "provider": "CONCERT"
			  },
			  "openChatUrl": "https://open.kakao.com/o/updated",
			  "openChatPassword": "5678"
			}
			""";

		mockMvc.perform(patch("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host))
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidPayload))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	@Test
	void 방장은_방을_닫을_수_있고_닫힌_방에는_신청할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(patch("/api/rooms/{roomId}/close", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomId").value(roomId))
			.andExpect(jsonPath("$.result.status").value("CLOSED"));

		mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomStatus").value("CLOSED"))
			.andExpect(jsonPath("$.result.permissions.canRequestJoin").value(false));

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "신청합니다"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ROOM01"));
	}

	@Test
	void 방_삭제는_방을_닫힌_상태로_전환한다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(delete("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomId").value(roomId))
			.andExpect(jsonPath("$.result.status").value("CLOSED"));

		mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomStatus").value("CLOSED"));
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
	void 입장_신청을_취소하면_다시_신청할_수_있다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		insertJoinRequest(roomId, applicant.getId(), "처음 신청");

		mockMvc.perform(delete("/api/rooms/{roomId}/join-requests/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("NOT_REQUESTED"));

		mockMvc.perform(get("/api/rooms/{roomId}/join-requests/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "다시 신청"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("PENDING"));
	}

	@Test
	void 승인된_입장_신청은_취소할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		Long requestId = insertJoinRequest(roomId, applicant.getId(), "처음 신청");
		jdbcTemplate.update("UPDATE join_requests SET status = 'APPROVED' WHERE id = ?", requestId);

		mockMvc.perform(delete("/api/rooms/{roomId}/join-requests/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("JOIN02"));
	}

	@Test
	void 거절된_입장_신청은_취소할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);
		Long requestId = insertJoinRequest(roomId, applicant.getId(), "처음 신청");
		jdbcTemplate.update("UPDATE join_requests SET status = 'REJECTED' WHERE id = ?", requestId);

		mockMvc.perform(delete("/api/rooms/{roomId}/join-requests/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("JOIN02"));
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
	void 멤버가_방을_나가면_오픈채팅을_볼_수_없고_다시_신청할_수_있다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 2);
		Long requestId = insertJoinRequest(roomId, applicant.getId(), "같이 이동하고 싶어요");
		insertMember(roomId, applicant.getId(), "MEMBER");
		jdbcTemplate.update("UPDATE join_requests SET status = 'APPROVED' WHERE id = ?", requestId);
		jdbcTemplate.update("UPDATE rooms SET status = 'FULL' WHERE id = ?", roomId);

		mockMvc.perform(delete("/api/rooms/{roomId}/members/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("LEFT"));

		mockMvc.perform(get("/api/rooms/{roomId}/open-chat", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant)))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/rooms/{roomId}", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(visitor)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.roomStatus").value("OPEN"))
			.andExpect(jsonPath("$.result.memberCount").value(1));

		mockMvc.perform(post("/api/rooms/{roomId}/join-requests", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(applicant))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("message", "다시 신청"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.status").value("PENDING"));
	}

	@Test
	void 방장은_방_나가기를_할_수_없다() throws Exception {
		Long roomId = insertRoom(host, "굿즈 같이 갈 분", 4);

		mockMvc.perform(delete("/api/rooms/{roomId}/members/me", roomId)
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("COMMON403"));
	}

	@Test
	void 내_방_목록을_조회한다() throws Exception {
		LocalDateTime activeStartAt = futureConcertStartAt();
		Long activeConcertId = insertConcert("active-my-room-test-concert", activeStartAt, activeStartAt.plusHours(2));
		Long hostedRoomId = insertRoom(host, activeConcertId, "내가 만든 방", 4);
		Long pendingRoomId = insertRoom(visitor, activeConcertId, "신청한 방", 4);
		insertJoinRequest(pendingRoomId, host.getId(), "신청 중");
		insertJoinRequest(hostedRoomId, applicant.getId(), "승인 대기");

		MvcResult result = mockMvc.perform(get("/api/me/rooms")
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString())
			.path("result")
			.path("items");
		JsonNode hostedRoom = findRoomItem(items, hostedRoomId);
		JsonNode pendingRoom = findRoomItem(items, pendingRoomId);
		assertThat(hostedRoom.path("viewerRole").asText()).isEqualTo("HOST");
		assertThat(hostedRoom.path("viewerJoinStatus").asText()).isEqualTo("APPROVED");
		assertThat(hostedRoom.path("roomStatus").asText()).isEqualTo("OPEN");
		assertThat(hostedRoom.path("concertTitle").asText()).isEqualTo("AURORA LIVE");
		assertThat(hostedRoom.path("concertStartAt").asText()).isEqualTo(RoomDateTimeFormatter.format(activeStartAt));
		assertThat(hostedRoom.path("daysUntilConcert").isNumber()).isTrue();
		assertThat(hostedRoom.path("venueName").asText()).isEqualTo("KSPO Dome");
		assertThat(hostedRoom.path("meetingAt").asText()).isEqualTo("2026-06-15T14:00:00+09:00");
		assertThat(hostedRoom.path("meetingPlaceName").asText()).isEqualTo("잠실역 5번 출구");
		assertThat(hostedRoom.path("meetingPlaceAddress").asText()).isEqualTo("서울 송파구 잠실동");
		assertThat(hostedRoom.path("memberCount").asLong()).isEqualTo(1);
		assertThat(hostedRoom.path("maxMembers").asInt()).isEqualTo(4);
		assertThat(hostedRoom.path("pendingJoinRequestCount").asLong()).isEqualTo(1);
		assertThat(pendingRoom.path("viewerJoinStatus").asText()).isEqualTo("PENDING");
		assertThat(pendingRoom.path("pendingJoinRequestCount").asLong()).isZero();
	}

	@Test
	void 내_방_목록은_공연이_종료된_방을_제외한다() throws Exception {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
		LocalDateTime activeStartAt = futureConcertStartAt();
		Long activeConcertId = insertConcert("active-room-test-concert", activeStartAt, activeStartAt.plusHours(2));
		Long expiredConcertId = insertConcert(
			"expired-room-test-concert",
			now.minusDays(2),
			now.minusDays(1)
		);
		Long activeRoomId = insertRoom(host, activeConcertId, "아직 참여중인 방", 4);
		Long expiredHostedRoomId = insertRoom(host, expiredConcertId, "지난 내가 만든 방", 4);
		Long expiredJoinedRoomId = insertRoom(visitor, expiredConcertId, "지난 참여 방", 4);
		Long expiredPendingRoomId = insertRoom(visitor, expiredConcertId, "지난 신청 방", 4);
		insertMember(expiredJoinedRoomId, host.getId(), "MEMBER");
		insertJoinRequest(expiredPendingRoomId, host.getId(), "지난 신청");

		MvcResult result = mockMvc.perform(get("/api/me/rooms")
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString())
			.path("result")
			.path("items");
		assertThat(roomIds(items))
			.contains(activeRoomId)
			.doesNotContain(expiredHostedRoomId, expiredJoinedRoomId, expiredPendingRoomId);
	}

	@Test
	void 내_방_목록은_tab으로_대기중인_방만_필터링한다() throws Exception {
		LocalDateTime activeStartAt = futureConcertStartAt();
		Long activeConcertId = insertConcert("active-pending-room-test-concert", activeStartAt, activeStartAt.plusHours(2));
		Long hostedRoomId = insertRoom(host, activeConcertId, "내가 만든 방", 4);
		Long pendingRoomId = insertRoom(visitor, activeConcertId, "신청한 방", 4);
		insertJoinRequest(pendingRoomId, host.getId(), "신청 중");

		MvcResult result = mockMvc.perform(get("/api/me/rooms")
				.param("tab", "pending")
				.header(HttpHeaders.AUTHORIZATION, bearer(host)))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString())
			.path("result")
			.path("items");
		assertThat(items).hasSize(1);
		assertThat(items.get(0).path("roomId").asLong()).isEqualTo(pendingRoomId);
		assertThat(items.get(0).path("roomId").asLong()).isNotEqualTo(hostedRoomId);
		assertThat(items.get(0).path("viewerJoinStatus").asText()).isEqualTo("PENDING");
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

	private Map<String, Object> updateRoomPayload(int maxMembers) {
		return Map.of(
			"title", "수정된 굿즈 동선 방",
			"description", "수정된 설명입니다.",
			"maxMembers", maxMembers,
			"roomTags", List.of("MEAL_TOGETHER"),
			"meetingAt", "2026-06-15T15:00:00+09:00",
			"meetingPlace", Map.of(
				"name", "잠실 종합운동장역",
				"address", "서울 송파구 올림픽로 지하 23",
				"lat", 37.510,
				"lng", 127.073,
				"provider", "KAKAO_ADDRESS"
			),
			"eventPlace", Map.of(
				"name", "KSPO Dome",
				"address", "서울 송파구 올림픽로 424",
				"lat", 37.519,
				"lng", 127.127,
				"provider", "CONCERT"
			),
			"openChatUrl", "https://open.kakao.com/o/updated",
			"openChatPassword", "5678"
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

	private User saveCompletedUser(String nickname) {
		User user = User.createKakao("kakao-" + nickname, nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile(nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		return userRepository.save(user);
	}

	private Long insertConcert() {
		return insertConcert(
			"room-test-concert",
			LocalDateTime.of(2026, 6, 15, 19, 0),
			LocalDateTime.of(2026, 6, 15, 21, 30)
		);
	}

	private Long insertConcert(String externalId, LocalDateTime startAt, LocalDateTime endAt) {
		jdbcTemplate.update("""
			INSERT INTO concerts (
				external_id, title, venue_name, start_at, end_at, lat, lng, source, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			externalId,
			"AURORA LIVE",
			"KSPO Dome",
			startAt,
			endAt,
			new BigDecimal("37.5190000"),
			new BigDecimal("127.1270000"),
			"SEED",
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject("SELECT id FROM concerts WHERE external_id = ?", Long.class, externalId);
	}

	private LocalDateTime futureConcertStartAt() {
		return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
			.plusDays(7)
			.withHour(19)
			.withMinute(0)
			.withSecond(0)
			.withNano(0);
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
		return insertRoom(hostUser, concertId, title, maxMembers);
	}

	private Long insertRoom(User hostUser, Long targetConcertId, String title, int maxMembers) {
		String openChatUrl = "https://open.kakao.com/o/test";
		jdbcTemplate.update("""
			INSERT INTO rooms (
				concert_id, host_user_id, title, description, max_members, meeting_at,
				meeting_place_id, event_place_id, open_chat_url, open_chat_password, status,
				created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			targetConcertId,
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

	private void insertMember(Long roomId, Long userId, String role) {
		jdbcTemplate.update(
			"INSERT INTO room_members (room_id, user_id, role, joined_at) VALUES (?, ?, ?, ?)",
			roomId,
			userId,
			role,
			LocalDateTime.now()
		);
	}

	private Long insertSchedule(Long roomId) {
		jdbcTemplate.update("""
			INSERT INTO schedules (room_id, created_at, updated_at) VALUES (?, ?, ?)
			""",
			roomId,
			LocalDateTime.now(),
			LocalDateTime.now()
		);
		return jdbcTemplate.queryForObject(
			"SELECT id FROM schedules WHERE room_id = ?",
			Long.class,
			roomId
		);
	}

	private void insertScheduleSlot(
		Long scheduleId,
		Long placeId,
		String slotType,
		String category,
		String title,
		int sortOrder,
		LocalDateTime startAt,
		LocalDateTime endAt,
		int dwellMinutes,
		boolean locked
	) {
		jdbcTemplate.update("""
			INSERT INTO schedule_slots (
				schedule_id, place_id, slot_type, category, title, sort_order,
				start_at, end_at, dwell_minutes, locked, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""",
			scheduleId,
			placeId,
			slotType,
			category,
			title,
			sortOrder,
			startAt,
			endAt,
			dwellMinutes,
			locked,
			LocalDateTime.now(),
			LocalDateTime.now()
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

	private JsonNode findRoomItem(JsonNode items, Long roomId) {
		for (JsonNode item : items) {
			if (item.path("roomId").asLong() == roomId) {
				return item;
			}
		}
		throw new AssertionError("room not found: " + roomId);
	}

	private List<Long> roomIds(JsonNode items) {
		List<Long> roomIds = new ArrayList<>();
		items.forEach(item -> roomIds.add(item.path("roomId").asLong()));
		return roomIds;
	}

	private JsonNode findMemberItem(JsonNode items, String nickname) {
		for (JsonNode item : items) {
			if (item.path("nickname").asText().equals(nickname)) {
				return item;
			}
		}
		throw new AssertionError("member not found: " + nickname);
	}

	private <T> T slotValue(Long scheduleId, int sortOrder, String column, Class<T> type) {
		return jdbcTemplate.queryForObject(
			"SELECT " + column + " FROM schedule_slots WHERE schedule_id = ? AND sort_order = ?",
			type,
			scheduleId,
			sortOrder
		);
	}

	private <T> T routeValue(Long scheduleId, String column, Class<T> type) {
		return jdbcTemplate.queryForObject(
			"SELECT " + column + " FROM route_segments WHERE schedule_id = ?",
			type,
			scheduleId
		);
	}

	private String slotPlaceName(Long scheduleId, int sortOrder) {
		return jdbcTemplate.queryForObject("""
			SELECT places.name
			FROM schedule_slots
			JOIN places ON places.id = schedule_slots.place_id
			WHERE schedule_slots.schedule_id = ? AND schedule_slots.sort_order = ?
			""",
			String.class,
			scheduleId,
			sortOrder
		);
	}

	private String bearer(User user) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));
	}
}
