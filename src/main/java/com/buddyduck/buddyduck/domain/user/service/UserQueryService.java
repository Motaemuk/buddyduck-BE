package com.buddyduck.buddyduck.domain.user.service;

import com.buddyduck.buddyduck.domain.room.enums.JoinRequestStatus;
import com.buddyduck.buddyduck.domain.room.repository.JoinRequestRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomMemberRepository;
import com.buddyduck.buddyduck.domain.user.dto.UserProfileResponse;
import com.buddyduck.buddyduck.domain.user.dto.UpdateProfileRequest;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final UserRepository userRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final JoinRequestRepository joinRequestRepository;

	@Transactional(readOnly = true)
	public UserProfileResponse getMyProfile(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

		return toUserProfileResponse(user);
	}

	@Transactional
	public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

		user.completeProfile(
			request.nickname(),
			request.ageRange(),
			request.gender()
		);

		return toUserProfileResponse(user);
	}

	private UserProfileResponse toUserProfileResponse(User user) {
		LocalDateTime now = LocalDateTime.now(KST);
		return new UserProfileResponse(
			user.getId(),
			user.getNickname(),
			user.getAgeRange(),
			user.getGender(),
			user.isProfileCompleted(),
			user.getAvatarColor(),
			roomMemberRepository.countActiveByUserId(user.getId(), now),
			joinRequestRepository.countActiveByUserIdAndStatus(user.getId(), JoinRequestStatus.PENDING, now)
		);
	}
}
