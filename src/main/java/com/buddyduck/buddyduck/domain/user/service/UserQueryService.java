package com.buddyduck.buddyduck.domain.user.service;

import com.buddyduck.buddyduck.domain.user.dto.UserProfileResponse;
import com.buddyduck.buddyduck.domain.user.dto.UpdateProfileRequest;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserProfileResponse getMyProfile(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

		return new UserProfileResponse(
			user.getId(),
			user.getNickname(),
			user.getAgeRange(),
			user.getGender(),
			user.isAgeVisible(),
			user.isGenderVisible(),
			user.isProfileCompleted(),
			user.getAvatarColor()
		);
	}

	@Transactional
	public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
		validateRequiredProfileInfo(request);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

		user.completeProfile(
			request.nickname(),
			request.ageRange(),
			request.gender(),
			request.ageVisible(),
			request.genderVisible()
		);

		return new UserProfileResponse(
			user.getId(),
			user.getNickname(),
			user.getAgeRange(),
			user.getGender(),
			user.isAgeVisible(),
			user.isGenderVisible(),
			user.isProfileCompleted(),
			user.getAvatarColor()
		);
	}

	private void validateRequiredProfileInfo(UpdateProfileRequest request) {
		if (request.ageRange() == AgeRange.PRIVATE || request.gender() == UserGender.PRIVATE) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
	}
}
