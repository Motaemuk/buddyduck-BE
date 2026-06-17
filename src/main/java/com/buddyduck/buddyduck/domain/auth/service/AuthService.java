package com.buddyduck.buddyduck.domain.auth.service;

import com.buddyduck.buddyduck.domain.auth.dto.KakaoLoginRequest;
import com.buddyduck.buddyduck.domain.auth.dto.LoginResponse;
import com.buddyduck.buddyduck.domain.auth.dto.LoginUserSummary;
import com.buddyduck.buddyduck.domain.auth.exception.AuthErrorCode;
import com.buddyduck.buddyduck.domain.auth.kakao.KakaoAuthClient;
import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import com.buddyduck.buddyduck.global.security.AuthUser;
import com.buddyduck.buddyduck.global.security.JwtTokenProvider;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final KakaoAuthClient kakaoAuthClient;
	private final JwtTokenProvider jwtTokenProvider;
	private final TransactionTemplate transactionTemplate;

	@Value("${kakao.oauth.allowed-redirect-uris}")
	private List<String> allowedRedirectUris;

	public LoginResponse loginWithKakao(KakaoLoginRequest request) {
		validateRedirectUri(request.redirectUri());
		KakaoUserInfo kakaoUserInfo = kakaoAuthClient.getUserInfo(request.code(), request.redirectUri());
		return Objects.requireNonNull(transactionTemplate.execute(status -> saveUserAndCreateResponse(kakaoUserInfo)));
	}

	private LoginResponse saveUserAndCreateResponse(KakaoUserInfo kakaoUserInfo) {
		User user = userRepository.findByKakaoId(kakaoUserInfo.kakaoId())
			.map(existingUser -> updateExistingUser(existingUser, kakaoUserInfo))
			.orElseGet(() -> createKakaoUser(kakaoUserInfo));
		boolean isNewUser = user.getId() == null;

		User savedUser = userRepository.save(user);
		String accessToken = jwtTokenProvider.createAccessToken(new AuthUser(savedUser.getId()));

		return new LoginResponse(
			accessToken,
			isNewUser,
			savedUser.isProfileCompleted(),
			new LoginUserSummary(savedUser.getId(), savedUser.getNickname())
		);
	}

	private void validateRedirectUri(String redirectUri) {
		boolean allowed = allowedRedirectUris.stream()
			.map(String::trim)
			.anyMatch(redirectUri::equals);
		if (!allowed) {
			throw new ProjectException(AuthErrorCode.INVALID_REDIRECT_URI);
		}
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
