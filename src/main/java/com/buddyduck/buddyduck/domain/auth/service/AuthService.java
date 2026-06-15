package com.buddyduck.buddyduck.domain.auth.service;

import com.buddyduck.buddyduck.domain.auth.dto.KakaoLoginRequest;
import com.buddyduck.buddyduck.domain.auth.dto.LoginResponse;
import com.buddyduck.buddyduck.domain.auth.dto.LoginUserSummary;
import com.buddyduck.buddyduck.domain.auth.kakao.KakaoAuthClient;
import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.security.AuthUser;
import com.buddyduck.buddyduck.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final KakaoAuthClient kakaoAuthClient;
	private final JwtTokenProvider jwtTokenProvider;

	@Transactional
	public LoginResponse loginWithKakao(KakaoLoginRequest request) {
		KakaoUserInfo kakaoUserInfo = kakaoAuthClient.getUserInfo(request.code(), request.redirectUri());
		User user = userRepository.findByKakaoId(kakaoUserInfo.kakaoId())
			.map(existingUser -> updateExistingUser(existingUser, kakaoUserInfo))
			.orElseGet(() -> createKakaoUser(kakaoUserInfo));
		boolean isNewUser = user.getId() == null;

		User savedUser = userRepository.save(user);
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(savedUser.getId()));

		return new LoginResponse(
			accessToken,
			isNewUser,
			new LoginUserSummary(savedUser.getId(), savedUser.getNickname())
		);
	}

	private User createKakaoUser(KakaoUserInfo kakaoUserInfo) {
		return User.createKakao(
			kakaoUserInfo.kakaoId(),
			kakaoUserInfo.nickname(),
			kakaoUserInfo.ageRange(),
			kakaoUserInfo.gender()
		);
	}

	private User updateExistingUser(User user, KakaoUserInfo kakaoUserInfo) {
		user.updateKakaoProfile(kakaoUserInfo.ageRange(), kakaoUserInfo.gender());
		return user;
	}
}
