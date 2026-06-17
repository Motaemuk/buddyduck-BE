package com.buddyduck.buddyduck.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private static final String SECRET_KEY = "01234567890123456789012345678901";

	@Test
	void access_token을_발급하고_user_id를_추출한다() {
		JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, Duration.ofHours(1).toMillis());

		String token = tokenProvider.createAccessToken(new AuthUser(1L));

		assertThat(tokenProvider.getUserId(token)).contains(1L);
	}

	@Test
	void 잘못된_token이면_empty를_반환한다() {
		JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, Duration.ofHours(1).toMillis());

		assertThat(tokenProvider.getUserId("invalid.token.value")).isEmpty();
	}

	@Test
	void 만료된_token이면_empty를_반환한다() {
		JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, -1L);

		String token = tokenProvider.createAccessToken(new AuthUser(1L));

		assertThat(tokenProvider.getUserId(token)).isEmpty();
	}

	@Test
	void user_id가_null이면_token을_발급하지_않는다() {
		JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET_KEY, Duration.ofHours(1).toMillis());

		assertThatThrownBy(() -> tokenProvider.createAccessToken(new AuthUser(null)))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("authUser.userId must not be null");
	}
}
