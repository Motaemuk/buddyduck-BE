package com.buddyduck.buddyduck.domain.auth.kakao;

import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KakaoUserInfoMapper {

	private static final String KAKAO_ACCOUNT = "kakao_account";
	private static final String PROFILE = "profile";
	private static final String DEFAULT_NICKNAME_PREFIX = "kakao_user_";

	public KakaoUserInfo map(Map<String, Object> attributes) {
		String kakaoId = requiredString(attributes, "id", "Kakao id is required.");
		Map<String, Object> kakaoAccount = nestedMap(attributes, KAKAO_ACCOUNT, "Kakao account is required.");
		Map<String, Object> profile = nestedMap(kakaoAccount, PROFILE, "Kakao profile is required.");

		String nickname = stringValue(profile, "nickname");
		if (nickname == null || nickname.isBlank()) {
			nickname = DEFAULT_NICKNAME_PREFIX + kakaoId;
		}

		return new KakaoUserInfo(
			kakaoId,
			nickname,
			mapAgeRange(requiredString(kakaoAccount, "age_range", "Kakao age_range is required.")),
			mapGender(requiredString(kakaoAccount, "gender", "Kakao gender is required."))
		);
	}

	private AgeRange mapAgeRange(String value) {
		return switch (value) {
			case "10~19" -> AgeRange.TEENS;
			case "20~29" -> AgeRange.TWENTIES;
			case "30~39" -> AgeRange.THIRTIES;
			default -> {
				if (value.matches("[4-9][0-9]~.*")) {
					yield AgeRange.FORTIES_PLUS;
				}
				throw new IllegalArgumentException("Unsupported Kakao age_range: " + value);
			}
		};
	}

	private UserGender mapGender(String value) {
		return switch (value.toLowerCase()) {
			case "female" -> UserGender.FEMALE;
			case "male" -> UserGender.MALE;
			default -> throw new IllegalArgumentException("Unsupported Kakao gender: " + value);
		};
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> nestedMap(Map<String, Object> source, String key, String message) {
		Object value = source.get(key);
		if (!(value instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException(message);
		}
		return (Map<String, Object>) map;
	}

	private String requiredString(Map<String, Object> source, String key, String message) {
		String value = stringValue(source, key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		return value;
	}

	private String stringValue(Map<String, Object> source, String key) {
		Object value = source.get(key);
		return value == null ? null : String.valueOf(value);
	}
}
