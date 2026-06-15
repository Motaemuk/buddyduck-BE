package com.buddyduck.buddyduck.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.buddyduck.buddyduck.domain.auth.dto.KakaoLoginRequest;
import com.buddyduck.buddyduck.domain.auth.dto.LoginResponse;
import com.buddyduck.buddyduck.domain.auth.kakao.KakaoAuthClient;
import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@MockBean
	private KakaoAuthClient kakaoAuthClient;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void Kakao_사용자가_없으면_회원가입하고_JWT를_발급한다() {
		given(kakaoAuthClient.getUserInfo("auth-code", "http://localhost:5173/oauth/kakao/callback"))
			.willReturn(new KakaoUserInfo("12345", "duck_fan", AgeRange.TWENTIES, UserGender.FEMALE));

		LoginResponse response = authService.loginWithKakao(
			new KakaoLoginRequest("auth-code", "http://localhost:5173/oauth/kakao/callback")
		);

		User savedUser = userRepository.findByKakaoId("12345").orElseThrow();
		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.isNewUser()).isTrue();
		assertThat(response.profileCompleted()).isFalse();
		assertThat(response.user().id()).isEqualTo(savedUser.getId());
		assertThat(response.user().nickname()).isEqualTo("duck_fan");
		assertThat(savedUser.getAgeRange()).isEqualTo(AgeRange.TWENTIES);
		assertThat(savedUser.getGender()).isEqualTo(UserGender.FEMALE);
	}

	@Test
	void 기존_Kakao_사용자는_닉네임을_덮어쓰지_않고_연령대와_성별만_갱신한다() {
		User existingUser = userRepository.save(User.createKakao(
			"12345",
			"local_duck",
			AgeRange.TWENTIES,
			UserGender.MALE
		));
		given(kakaoAuthClient.getUserInfo("auth-code", "http://localhost:5173/oauth/kakao/callback"))
			.willReturn(new KakaoUserInfo("12345", "kakao_changed", AgeRange.THIRTIES, UserGender.FEMALE));

		LoginResponse response = authService.loginWithKakao(
			new KakaoLoginRequest("auth-code", "http://localhost:5173/oauth/kakao/callback")
		);

		User updatedUser = userRepository.findById(existingUser.getId()).orElseThrow();
		assertThat(response.isNewUser()).isFalse();
		assertThat(response.profileCompleted()).isFalse();
		assertThat(response.user().nickname()).isEqualTo("local_duck");
		assertThat(updatedUser.getNickname()).isEqualTo("local_duck");
		assertThat(updatedUser.getAgeRange()).isEqualTo(AgeRange.THIRTIES);
		assertThat(updatedUser.getGender()).isEqualTo(UserGender.FEMALE);
	}

	@Test
	void 프로필을_완료한_Kakao_사용자는_로그인해도_추가정보를_덮어쓰지_않는다() {
		User existingUser = User.createKakao(
			"12345",
			"local_duck",
			AgeRange.TWENTIES,
			UserGender.MALE
		);
		existingUser.completeProfile("local_duck", AgeRange.TWENTIES, UserGender.MALE, false, true);
		userRepository.save(existingUser);
		given(kakaoAuthClient.getUserInfo("auth-code", "http://localhost:5173/oauth/kakao/callback"))
			.willReturn(new KakaoUserInfo("12345", "kakao_changed", AgeRange.THIRTIES, UserGender.FEMALE));

		LoginResponse response = authService.loginWithKakao(
			new KakaoLoginRequest("auth-code", "http://localhost:5173/oauth/kakao/callback")
		);

		User updatedUser = userRepository.findById(existingUser.getId()).orElseThrow();
		assertThat(response.isNewUser()).isFalse();
		assertThat(response.profileCompleted()).isTrue();
		assertThat(updatedUser.getNickname()).isEqualTo("local_duck");
		assertThat(updatedUser.getAgeRange()).isEqualTo(AgeRange.TWENTIES);
		assertThat(updatedUser.getGender()).isEqualTo(UserGender.MALE);
		assertThat(updatedUser.isAgeVisible()).isFalse();
		assertThat(updatedUser.isGenderVisible()).isTrue();
	}

	@Test
	void Kakao_연령대와_성별이_PRIVATE이어도_회원가입한다() {
		given(kakaoAuthClient.getUserInfo("auth-code", "http://localhost:5173/oauth/kakao/callback"))
			.willReturn(new KakaoUserInfo("12345", "duck_fan", AgeRange.PRIVATE, UserGender.PRIVATE));

		LoginResponse response = authService.loginWithKakao(
			new KakaoLoginRequest("auth-code", "http://localhost:5173/oauth/kakao/callback")
		);

		User savedUser = userRepository.findByKakaoId("12345").orElseThrow();
		assertThat(response.isNewUser()).isTrue();
		assertThat(response.profileCompleted()).isFalse();
		assertThat(savedUser.getAgeRange()).isEqualTo(AgeRange.PRIVATE);
		assertThat(savedUser.getGender()).isEqualTo(UserGender.PRIVATE);
	}
}
