package com.buddyduck.buddyduck.domain.room.service;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertInterestTagRepository;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestApproveResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestCreateRequest;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestCreateResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestDecisionResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestListItemResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestListResponse;
import com.buddyduck.buddyduck.domain.room.dto.MyJoinRequestStatusResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomDateTimeFormatter;
import com.buddyduck.buddyduck.domain.room.entity.JoinRequest;
import com.buddyduck.buddyduck.domain.room.entity.Room;
import com.buddyduck.buddyduck.domain.room.entity.RoomMember;
import com.buddyduck.buddyduck.domain.room.entity.RoomTag;
import com.buddyduck.buddyduck.domain.room.enums.JoinRequestStatus;
import com.buddyduck.buddyduck.domain.room.enums.RoomMemberRole;
import com.buddyduck.buddyduck.domain.room.enums.RoomStatus;
import com.buddyduck.buddyduck.domain.room.exception.RoomErrorCode;
import com.buddyduck.buddyduck.domain.room.repository.JoinRequestRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomMemberRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomTagRepository;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JoinRequestService {

	private static final int MAX_PAGE_SIZE = 50;

	private final RoomService roomService;
	private final JoinRequestRepository joinRequestRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomTagRepository roomTagRepository;
	private final ConcertInterestTagRepository concertInterestTagRepository;

	@Transactional
	public JoinRequestCreateResponse createJoinRequest(Long roomId, Long userId, JoinRequestCreateRequest request) {
		Room room = roomService.getRoomOrThrow(roomId);
		User user = roomService.getUserOrThrow(userId);

		if (roomService.isHost(room, userId)
			|| roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)
			|| joinRequestRepository.existsByRoomIdAndUserId(roomId, userId)) {
			throw new ProjectException(RoomErrorCode.DUPLICATE_JOIN);
		}
		if (room.getStatus() != RoomStatus.OPEN || roomService.isFull(room)) {
			throw new ProjectException(RoomErrorCode.ROOM_FULL);
		}

		JoinRequest joinRequest = joinRequestRepository.save(JoinRequest.create(room, user, request.message()));
		return new JoinRequestCreateResponse(joinRequest.getId(), joinRequest.getStatus().name());
	}

	@Transactional(readOnly = true)
	public MyJoinRequestStatusResponse getMyJoinRequest(Long roomId, Long userId) {
		roomService.getRoomOrThrow(roomId);
		return joinRequestRepository.findByRoomIdAndUserId(roomId, userId)
			.map(request -> new MyJoinRequestStatusResponse(request.getStatus().name(), request.getMessage()))
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public JoinRequestListResponse getJoinRequests(
		Long roomId,
		Long userId,
		JoinRequestStatus status,
		int page,
		int size
	) {
		Room room = roomService.getRoomOrThrow(roomId);
		requireHost(room, userId);
		if (page < 0 || size <= 0) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}

		JoinRequestStatus statusFilter = status == null ? JoinRequestStatus.PENDING : status;
		List<JoinRequestListItemResponse> allItems = joinRequestRepository
			.findAllByRoomIdAndStatusOrderByCreatedAtAsc(roomId, statusFilter)
			.stream()
			.map(request -> toJoinRequestListItem(room, request))
			.toList();

		int limitedSize = Math.min(size, MAX_PAGE_SIZE);
		int fromIndex = Math.min(page * limitedSize, allItems.size());
		int toIndex = Math.min(fromIndex + limitedSize, allItems.size());
		return new JoinRequestListResponse(
			allItems.subList(fromIndex, toIndex),
			page,
			limitedSize,
			toIndex < allItems.size()
		);
	}

	@Transactional
	public JoinRequestApproveResponse approveJoinRequest(Long roomId, Long requestId, Long userId) {
		Room room = roomService.getRoomOrThrow(roomId);
		User host = roomService.getUserOrThrow(userId);
		requireHost(room, userId);
		JoinRequest joinRequest = getJoinRequestOrThrow(roomId, requestId);
		if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
			throw new ProjectException(RoomErrorCode.INVALID_JOIN_STATUS);
		}
		if (room.getStatus() != RoomStatus.OPEN || roomService.isFull(room)) {
			throw new ProjectException(RoomErrorCode.ROOM_FULL);
		}

		joinRequest.approve(host);
		RoomMember member = roomMemberRepository.save(RoomMember.create(
			room,
			joinRequest.getUser(),
			RoomMemberRole.MEMBER
		));
		if (roomService.isFull(room)) {
			room.markFull();
		}

		return new JoinRequestApproveResponse(joinRequest.getId(), joinRequest.getStatus().name(), member.getId());
	}

	@Transactional
	public JoinRequestDecisionResponse rejectJoinRequest(Long roomId, Long requestId, Long userId) {
		Room room = roomService.getRoomOrThrow(roomId);
		User host = roomService.getUserOrThrow(userId);
		requireHost(room, userId);
		JoinRequest joinRequest = getJoinRequestOrThrow(roomId, requestId);
		if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
			throw new ProjectException(RoomErrorCode.INVALID_JOIN_STATUS);
		}

		joinRequest.reject(host);
		return new JoinRequestDecisionResponse(joinRequest.getId(), joinRequest.getStatus().name());
	}

	private JoinRequest getJoinRequestOrThrow(Long roomId, Long requestId) {
		JoinRequest joinRequest = joinRequestRepository.findById(requestId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
		if (!joinRequest.getRoom().getId().equals(roomId)) {
			throw new ProjectException(GeneralErrorCode.NOT_FOUND);
		}
		return joinRequest;
	}

	private void requireHost(Room room, Long userId) {
		if (!roomService.isHost(room, userId)) {
			throw new ProjectException(GeneralErrorCode.FORBIDDEN);
		}
	}

	private JoinRequestListItemResponse toJoinRequestListItem(Room room, JoinRequest request) {
		User user = request.getUser();
		Set<InterestTag> roomTags = roomTagRepository.findAllByRoomIdOrderByIdAsc(room.getId())
			.stream()
			.map(RoomTag::getTag)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		List<InterestTag> matchedTags = concertInterestTagRepository
			.findAllByUserIdAndConcertIdOrderByIdAsc(user.getId(), room.getConcert().getId())
			.stream()
			.map(com.buddyduck.buddyduck.domain.concert.entity.ConcertInterestTag::getTag)
			.filter(roomTags::contains)
			.toList();

		return new JoinRequestListItemResponse(
			request.getId(),
			user.getId(),
			user.getNickname(),
			user.isAgeVisible() ? user.getAgeRange() : AgeRange.PRIVATE,
			user.isGenderVisible() ? user.getGender() : UserGender.PRIVATE,
			request.getMessage(),
			matchedTags,
			RoomDateTimeFormatter.format(request.getCreatedAt())
		);
	}
}
