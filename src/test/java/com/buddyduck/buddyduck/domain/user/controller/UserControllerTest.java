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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
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
			.andExpect(jsonPath("$.result.ageVisible").value(false))
			.andExpect(jsonPath("$.result.genderVisible").value(false))
			.andExpect(jsonPath("$.result.profileCompleted").value(false))
			.andExpect(jsonPath("$.result.avatarColor").value("#FACC15"));
	}

	@Test
	void 추가_프로필을_저장하면_회원가입이_완료된다() throws Exception {
		User user = userRepository.save(User.createKakao(
			"12345",
			"duck_fan",
			AgeRange.PRIVATE,
			UserGender.PRIVATE
		));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"nickname", "moon_armies",
					"ageRange", "TWENTIES",
					"gender", "FEMALE",
					"ageVisible", false,
					"genderVisible", true
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.nickname").value("moon_armies"))
			.andExpect(jsonPath("$.result.ageRange").value("TWENTIES"))
			.andExpect(jsonPath("$.result.gender").value("FEMALE"))
			.andExpect(jsonPath("$.result.ageVisible").value(false))
			.andExpect(jsonPath("$.result.genderVisible").value(true))
			.andExpect(jsonPath("$.result.profileCompleted").value(true));
	}

	@Test
	void 추가_프로필에서_PRIVATE_연령대는_400을_응답한다() throws Exception {
		User user = userRepository.save(User.createKakao(
			"12345",
			"duck_fan",
			AgeRange.PRIVATE,
			UserGender.PRIVATE
		));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"nickname", "moon_armies",
					"ageRange", "PRIVATE",
					"gender", "FEMALE",
					"ageVisible", false,
					"genderVisible", true
				))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}

	@Test
	void 추가_프로필에서_PRIVATE_성별은_400을_응답한다() throws Exception {
		User user = userRepository.save(User.createKakao(
			"12345",
			"duck_fan",
			AgeRange.PRIVATE,
			UserGender.PRIVATE
		));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"nickname", "moon_armies",
					"ageRange", "TWENTIES",
					"gender", "PRIVATE",
					"ageVisible", false,
					"genderVisible", true
				))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}
}
