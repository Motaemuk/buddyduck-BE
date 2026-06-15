package com.buddyduck.buddyduck.domain.user.service;

import com.buddyduck.buddyduck.domain.user.dto.UserProfileResponse;
import com.buddyduck.buddyduck.domain.user.entity.User;
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
			user.getAvatarColor()
		);
	}
}
