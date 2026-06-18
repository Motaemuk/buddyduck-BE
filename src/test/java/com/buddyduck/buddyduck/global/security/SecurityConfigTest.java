package com.buddyduck.buddyduck.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:5173,https://boostad.site")
@AutoConfigureMockMvc
class SecurityConfigTest {

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
	void health_API는_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(get("/api/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.status").value("UP"));
	}

	@Test
	void OpenAPI_JSON은_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.info.title").value("Buddyduck API"))
			.andExpect(jsonPath("$.info.version").value("v1"));

		mockMvc.perform(get("/v3/api-docs.yaml"))
			.andExpect(status().isOk());
	}

	@Test
	void 프로필이_완료되지_않아도_OpenAPI_문서는_호출할_수_있다() throws Exception {
		User user = userRepository.save(User.createKakao("kakao-openapi", "openapi_duck", null, null));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(get("/v3/api-docs")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists());

		mockMvc.perform(get("/v3/api-docs.yaml")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk());
	}

	@Test
	void Swagger_UI는_인증_없이_호출할_수_있다() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk());
	}

	@Test
	void 허용된_origin의_preflight_요청은_인증_없이_통과한다() throws Exception {
		mockMvc.perform(options("/api/rooms")
				.header(HttpHeaders.ORIGIN, "https://boostad.site")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://boostad.site"));
	}

	@Test
	void 프로필이_완료되지_않은_사용자는_보호_API를_호출할_수_없다() throws Exception {
		User user = userRepository.save(User.createKakao("kakao-guard", "guard_duck", null, null));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(post("/api/rooms")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED_PROFILE_INFO"));
	}

	@Test
	void 프로필이_완료되지_않아도_내_프로필_조회와_추가_프로필_저장은_허용한다() throws Exception {
		User user = userRepository.save(User.createKakao("kakao-profile", "profile_duck", null, null));
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(user.getId()));

		mockMvc.perform(get("/api/users/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.profileCompleted").value(false));

		mockMvc.perform(patch("/api/users/me/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"nickname", "profile_duck",
					"ageRange", "TWENTIES",
					"gender", "FEMALE"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.profileCompleted").value(true));
	}

	@Test
	void 프로필이_완료된_사용자는_보호_API_진입이_허용된다() throws Exception {
		User user = User.createKakao("kakao-complete", "complete_duck", AgeRange.TWENTIES, UserGender.FEMALE);
		user.completeProfile("complete_duck", AgeRange.TWENTIES, UserGender.FEMALE);
		User savedUser = userRepository.save(user);
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(savedUser.getId()));

		mockMvc.perform(post("/api/rooms")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}
}
