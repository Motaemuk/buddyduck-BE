package com.buddyduck.buddyduck.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.domain.auth.dto.KakaoLoginRequest;
import com.buddyduck.buddyduck.domain.auth.dto.LoginResponse;
import com.buddyduck.buddyduck.domain.auth.dto.LoginUserSummary;
import com.buddyduck.buddyduck.domain.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private AuthService authService;

	@Test
	void Kakao_로그인에_성공하면_JWT와_사용자_요약을_응답한다() throws Exception {
		given(authService.loginWithKakao(any(KakaoLoginRequest.class)))
			.willReturn(new LoginResponse(
				"access-token",
				true,
				false,
				new LoginUserSummary(1L, "duck_fan")
			));

		mockMvc.perform(post("/api/auth/kakao")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"code", "auth-code",
					"redirectUri", "http://localhost:5173/oauth/kakao/callback"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.code").value("COMMON200"))
			.andExpect(jsonPath("$.result.accessToken").value("access-token"))
			.andExpect(jsonPath("$.result.isNewUser").value(true))
			.andExpect(jsonPath("$.result.profileCompleted").value(false))
			.andExpect(jsonPath("$.result.user.id").value(1))
			.andExpect(jsonPath("$.result.user.nickname").value("duck_fan"));
	}

	@Test
	void Kakao_로그인_요청에_redirectUri가_없으면_400을_응답한다() throws Exception {
		mockMvc.perform(post("/api/auth/kakao")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"code", "auth-code"
				))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON400"));
	}
}
