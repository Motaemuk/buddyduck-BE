package com.buddyduck.buddyduck.domain.place.controller;

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
class PlaceControllerTest {

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

	private User user;

	@BeforeEach
	void setUp() {
		jdbcTemplate.update("DELETE FROM places");
		userRepository.deleteAll();
		user = saveCompletedUser("duck_fan");
	}

	@Test
	void 장소_검색은_인증이_필요하다() throws Exception {
		mockMvc.perform(get("/api/places/search").param("keyword", "잠실"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("COMMON401"));
	}

	@Test
	void 장소명을_검색하면_DB_장소_후보를_반환한다() throws Exception {
		insertPlace("KAKAO_KEYWORD", "place-1", "잠실 카페 무드", "서울 송파구 올림픽로 300");
		insertPlace("KAKAO_KEYWORD", "place-2", "부산 카페", "부산 해운대구");

		mockMvc.perform(get("/api/places/search")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.param("keyword", "잠실"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.result.items.length()").value(1))
			.andExpect(jsonPath("$.result.items[0].name").value("잠실 카페 무드"))
			.andExpect(jsonPath("$.result.items[0].address").value("서울 송파구 올림픽로 300"))
			.andExpect(jsonPath("$.result.items[0].lat").value(37.515))
			.andExpect(jsonPath("$.result.items[0].lng").value(127.102))
			.andExpect(jsonPath("$.result.items[0].provider").value("KAKAO_KEYWORD"));
	}

	@Test
	void 주소를_좌표로_변환하면_DB_주소_후보를_반환한다() throws Exception {
		insertPlace("KAKAO_ADDRESS", "addr-1", "KSPO Dome", "서울 송파구 올림픽로 424");

		mockMvc.perform(get("/api/places/geocode")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.param("address", "올림픽로 424"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.items[0].address").value("서울 송파구 올림픽로 424"))
			.andExpect(jsonPath("$.result.items[0].lat").value(37.515))
			.andExpect(jsonPath("$.result.items[0].lng").value(127.102))
			.andExpect(jsonPath("$.result.items[0].provider").value("KAKAO_ADDRESS"));
	}

	@Test
	void 선택한_장소를_upsert하면_placeId를_반환한다() throws Exception {
		String payload = objectMapper.writeValueAsString(Map.of(
			"provider", "KAKAO_KEYWORD",
			"providerPlaceId", "123456",
			"name", "잠실 카페 무드",
			"address", "서울 송파구 올림픽로 300",
			"lat", 37.515,
			"lng", 127.102
		));

		mockMvc.perform(post("/api/places")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.placeId").isNumber());

		mockMvc.perform(post("/api/places")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.result.placeId").isNumber());

		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM places WHERE provider = ? AND provider_place_id = ?",
			Integer.class,
			"KAKAO_KEYWORD",
			"123456"
		);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void 빈_검색어는_400을_응답한다() throws Exception {
		mockMvc.perform(get("/api/places/search")
				.header(HttpHeaders.AUTHORIZATION, bearer())
				.param("keyword", " "))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	private void insertPlace(String provider, String providerPlaceId, String name, String address) {
		jdbcTemplate.update("""
			INSERT INTO places (
				provider, provider_place_id, name, address, lat, lng, created_at, updated_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""",
			provider,
			providerPlaceId,
			name,
			address,
			new BigDecimal("37.5150000"),
			new BigDecimal("127.1020000"),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
	}

	private User saveCompletedUser(String nickname) {
		User user = User.createKakao("kakao-" + nickname, nickname, AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile(nickname, AgeRange.TWENTIES, UserGender.FEMALE, true, true);
		return userRepository.save(user);
	}

	private String bearer() {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));
	}
}
