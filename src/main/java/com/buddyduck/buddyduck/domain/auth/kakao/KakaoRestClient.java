package com.buddyduck.buddyduck.domain.auth.kakao;

import com.buddyduck.buddyduck.domain.auth.exception.AuthErrorCode;
import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class KakaoRestClient implements KakaoAuthClient {

	private static final String GRANT_TYPE = "authorization_code";
	private static final String BEARER_PREFIX = "Bearer ";

	private final RestClient.Builder restClientBuilder;
	private final KakaoUserInfoMapper kakaoUserInfoMapper;

	@Value("${kakao.oauth.client-id}")
	private String clientId;

	@Value("${kakao.oauth.client-secret:}")
	private String clientSecret;

	@Value("${kakao.oauth.token-uri}")
	private String tokenUri;

	@Value("${kakao.oauth.user-info-uri}")
	private String userInfoUri;

	@Override
	public KakaoUserInfo getUserInfo(String code, String redirectUri) {
		try {
			KakaoTokenResponse tokenResponse = requestToken(code, redirectUri);
			Map<String, Object> attributes = requestUserInfo(tokenResponse.accessToken());
			return kakaoUserInfoMapper.map(attributes);
		} catch (IllegalArgumentException exception) {
			throw new ProjectException(AuthErrorCode.EXTERNAL_ERROR);
		} catch (RestClientException exception) {
			throw new ProjectException(AuthErrorCode.EXTERNAL_ERROR);
		}
	}

	private KakaoTokenResponse requestToken(String code, String redirectUri) {
		KakaoTokenResponse tokenResponse = restClientBuilder.build()
			.post()
			.uri(tokenUri)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(createTokenRequest(code, redirectUri))
			.retrieve()
			.body(KakaoTokenResponse.class);

		if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
			throw new ProjectException(AuthErrorCode.EXTERNAL_ERROR);
		}
		return tokenResponse;
	}

	private MultiValueMap<String, String> createTokenRequest(String code, String redirectUri) {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("grant_type", GRANT_TYPE);
		formData.add("client_id", clientId);
		formData.add("redirect_uri", redirectUri);
		formData.add("code", code);
		if (StringUtils.hasText(clientSecret)) {
			formData.add("client_secret", clientSecret);
		}
		return formData;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> requestUserInfo(String accessToken) {
		Map<String, Object> attributes = restClientBuilder.build()
			.get()
			.uri(userInfoUri)
			.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.body(Map.class);

		if (attributes == null) {
			throw new ProjectException(AuthErrorCode.EXTERNAL_ERROR);
		}
		return attributes;
	}

	private record KakaoTokenResponse(
		@JsonProperty("access_token")
		String accessToken
	) {
	}
}
