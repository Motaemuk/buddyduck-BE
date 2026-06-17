package com.buddyduck.buddyduck.domain.auth.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import com.buddyduck.buddyduck.domain.auth.kakao.dto.KakaoUserInfo;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KakaoUserInfoMapperTest {

	private final KakaoUserInfoMapper mapper = new KakaoUserInfoMapper();

	@Test
	void Kakao_응답에서_사용자_필수_정보를_추출한다() {
		KakaoUserInfo userInfo = mapper.map(userAttributes("12345", "duck_fan", "20~29", "female"));

		assertThat(userInfo.kakaoId()).isEqualTo("12345");
		assertThat(userInfo.nickname()).isEqualTo("duck_fan");
		assertThat(userInfo.ageRange()).isEqualTo(AgeRange.TWENTIES);
		assertThat(userInfo.gender()).isEqualTo(UserGender.FEMALE);
	}

	@Test
	void 연령대는_서비스_enum으로_변환한다() {
		assertThat(mapper.map(userAttributes("1", "teen", "10~19", "male")).ageRange())
			.isEqualTo(AgeRange.TEENS);
		assertThat(mapper.map(userAttributes("2", "twenties", "20~29", "male")).ageRange())
			.isEqualTo(AgeRange.TWENTIES);
		assertThat(mapper.map(userAttributes("3", "thirties", "30~39", "male")).ageRange())
			.isEqualTo(AgeRange.THIRTIES);
		assertThat(mapper.map(userAttributes("4", "forties", "40~49", "male")).ageRange())
			.isEqualTo(AgeRange.FORTIES_PLUS);
		assertThat(mapper.map(userAttributes("5", "fifties", "50~59", "male")).ageRange())
			.isEqualTo(AgeRange.FORTIES_PLUS);
	}

	@Test
	void 성별은_서비스_enum으로_변환한다() {
		assertThat(mapper.map(userAttributes("1", "female", "20~29", "female")).gender())
			.isEqualTo(UserGender.FEMALE);
		assertThat(mapper.map(userAttributes("2", "male", "20~29", "male")).gender())
			.isEqualTo(UserGender.MALE);
	}

	@Test
	void 닉네임이_비어있으면_kakao_id_기반_닉네임을_사용한다() {
		KakaoUserInfo userInfo = mapper.map(userAttributes("12345", " ", "20~29", "male"));

		assertThat(userInfo.nickname()).isEqualTo("kakao_user_12345");
	}

	@Test
	void 연령대가_없으면_null로_매핑한다() {
		Map<String, Object> attributes = userAttributes("12345", "duck_fan", null, "female");

		KakaoUserInfo userInfo = mapper.map(attributes);

		assertThat(userInfo.ageRange()).isNull();
	}

	@Test
	void 성별이_없으면_null로_매핑한다() {
		Map<String, Object> attributes = userAttributes("12345", "duck_fan", "20~29", null);

		KakaoUserInfo userInfo = mapper.map(attributes);

		assertThat(userInfo.gender()).isNull();
	}

	private Map<String, Object> userAttributes(String id, String nickname, String ageRange, String gender) {
		Map<String, Object> profile = new HashMap<>();
		profile.put("nickname", nickname);

		Map<String, Object> kakaoAccount = new HashMap<>();
		kakaoAccount.put("profile", profile);
		if (ageRange != null) {
			kakaoAccount.put("age_range", ageRange);
		}
		if (gender != null) {
			kakaoAccount.put("gender", gender);
		}

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("id", id);
		attributes.put("kakao_account", kakaoAccount);
		return attributes;
	}
}
