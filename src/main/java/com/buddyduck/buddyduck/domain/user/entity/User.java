package com.buddyduck.buddyduck.domain.user.entity;

import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "kakao_id", length = 100, unique = true)
	private String kakaoId;

	@Column(nullable = false, length = 30)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "age_range", nullable = false, length = 20)
	private AgeRange ageRange = AgeRange.PRIVATE;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserGender gender = UserGender.PRIVATE;

	@Column(name = "avatar_color", nullable = false, length = 20)
	private String avatarColor = "#FACC15";

	@Column(name = "profile_completed", nullable = false)
	private boolean profileCompleted = false;

	@Column(name = "age_visible", nullable = false)
	private boolean ageVisible = false;

	@Column(name = "gender_visible", nullable = false)
	private boolean genderVisible = false;

	public static User createKakao(String kakaoId, String nickname, AgeRange ageRange, UserGender gender) {
		User user = new User();
		user.kakaoId = kakaoId;
		user.nickname = nickname;
		user.ageRange = ageRange;
		user.gender = gender;
		return user;
	}

	public void updateKakaoProfile(AgeRange ageRange, UserGender gender) {
		if (profileCompleted) {
			return;
		}
		this.ageRange = ageRange;
		this.gender = gender;
	}

	public void completeProfile(
		String nickname,
		AgeRange ageRange,
		UserGender gender,
		boolean ageVisible,
		boolean genderVisible
	) {
		this.nickname = nickname;
		this.ageRange = ageRange;
		this.gender = gender;
		this.ageVisible = ageVisible;
		this.genderVisible = genderVisible;
		this.profileCompleted = true;
	}
}
