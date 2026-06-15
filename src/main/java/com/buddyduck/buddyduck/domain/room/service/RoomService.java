package com.buddyduck.buddyduck.domain.room.service;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertInterestTagRepository;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.repository.PlaceRepository;
import com.buddyduck.buddyduck.domain.room.dto.CreateRoomRequest;
import com.buddyduck.buddyduck.domain.room.dto.CreateRoomResponse;
import com.buddyduck.buddyduck.domain.room.dto.MyRoomItemResponse;
import com.buddyduck.buddyduck.domain.room.dto.MyRoomListResponse;
import com.buddyduck.buddyduck.domain.room.dto.OpenChatResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomDateTimeFormatter;
import com.buddyduck.buddyduck.domain.room.dto.RoomDetailResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomListItemResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomListResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomPermissionsResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomPlaceRequest;
import com.buddyduck.buddyduck.domain.room.entity.JoinRequest;
import com.buddyduck.buddyduck.domain.room.entity.Room;
import com.buddyduck.buddyduck.domain.room.entity.RoomMember;
import com.buddyduck.buddyduck.domain.room.entity.RoomTag;
import com.buddyduck.buddyduck.domain.room.enums.JoinRequestStatus;
import com.buddyduck.buddyduck.domain.room.enums.RoomMemberRole;
import com.buddyduck.buddyduck.domain.room.enums.RoomStatus;
import com.buddyduck.buddyduck.domain.room.repository.JoinRequestRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomMemberRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomTagRepository;
import com.buddyduck.buddyduck.domain.schedule.entity.Schedule;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleRepository;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoomService {

	private static final int MAX_PAGE_SIZE = 50;

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomTagRepository roomTagRepository;
	private final JoinRequestRepository joinRequestRepository;
	private final ConcertRepository concertRepository;
	private final ConcertInterestTagRepository concertInterestTagRepository;
	private final PlaceRepository placeRepository;
	private final ScheduleRepository scheduleRepository;
	private final UserRepository userRepository;

	@Transactional
	public CreateRoomResponse createRoom(Long userId, CreateRoomRequest request) {
		if (request.maxMembers() < 2) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		User host = getUserOrThrow(userId);
		Concert concert = getConcertOrThrow(request.concertId());
		Place meetingPlace = upsertPlace(request.meetingPlace());
		Place eventPlace = upsertPlace(request.eventPlace());

		Room room = roomRepository.save(Room.create(
			concert,
			host,
			request.title(),
			request.description(),
			request.maxMembers(),
			request.meetingAt().toLocalDateTime(),
			meetingPlace,
			eventPlace,
			request.openChatUrl(),
			request.openChatPassword()
		));
		roomMemberRepository.save(RoomMember.create(room, host, RoomMemberRole.HOST));
		roomTagRepository.saveAll(deduplicate(request.roomTags()).stream()
			.map(tag -> RoomTag.create(room, tag))
			.toList());
		Schedule schedule = scheduleRepository.save(Schedule.create(room));

		return new CreateRoomResponse(room.getId(), schedule.getId());
	}

	@Transactional(readOnly = true)
	public RoomListResponse getRoomsByConcert(
		Long concertId,
		Long userId,
		String status,
		String tags,
		Integer minMatchCount,
		int page,
		int size
	) {
		getConcertOrThrow(concertId);
		if (page < 0 || size <= 0) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		RoomStatus statusFilter = parseRoomStatus(status);
		Set<InterestTag> tagFilter = parseTags(tags);
		Set<InterestTag> userTags = getUserInterestTags(userId, concertId);

		List<RoomListItemResponse> allItems = roomRepository.findAllByConcertIdOrderByCreatedAtDesc(concertId)
			.stream()
			.filter(room -> statusFilter == null || room.getStatus() == statusFilter)
			.map(room -> toRoomListItem(room, userTags))
			.filter(item -> tagFilter.isEmpty() || item.roomTags().containsAll(tagFilter))
			.filter(item -> minMatchCount == null || item.matchCount() >= minMatchCount)
			.toList();

		return page(allItems, page, Math.min(size, MAX_PAGE_SIZE));
	}

	@Transactional(readOnly = true)
	public RoomDetailResponse getRoom(Long roomId, Long userId) {
		Room room = getRoomOrThrow(roomId);
		ViewerState viewerState = resolveViewerState(room, userId);
		boolean full = isFull(room);
		boolean host = viewerState.role().equals("HOST");
		boolean approved = viewerState.joinStatus().equals("APPROVED");
		RoomPermissionsResponse permissions = new RoomPermissionsResponse(
			viewerState.role().equals("VISITOR") && viewerState.joinStatus().equals("NOT_REQUESTED") && !full && room.getStatus() == RoomStatus.OPEN,
			host,
			host || approved,
			host || approved,
			host
		);
		long pendingRequestCount = joinRequestRepository.countByRoomIdAndStatus(roomId, JoinRequestStatus.PENDING);

		return new RoomDetailResponse(
			room.getId(),
			room.getTitle(),
			viewerState.role(),
			viewerState.joinStatus(),
			permissions,
			pendingRequestCount
		);
	}

	@Transactional(readOnly = true)
	public MyRoomListResponse getMyRooms(Long userId, String tab) {
		getUserOrThrow(userId);
		List<MyRoomItemResponse> items = new ArrayList<>();

		roomRepository.findAllByHostUserIdOrderByCreatedAtDesc(userId)
			.forEach(room -> items.add(new MyRoomItemResponse(room.getId(), room.getTitle(), "HOST", "APPROVED")));

		roomMemberRepository.findAllByUserIdOrderByJoinedAtDesc(userId)
			.stream()
			.filter(member -> member.getRole() == RoomMemberRole.MEMBER)
			.forEach(member -> items.add(new MyRoomItemResponse(
				member.getRoom().getId(),
				member.getRoom().getTitle(),
				"MEMBER",
				"APPROVED"
			)));

		joinRequestRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
			.stream()
			.filter(request -> request.getStatus() == JoinRequestStatus.PENDING)
			.forEach(request -> items.add(new MyRoomItemResponse(
				request.getRoom().getId(),
				request.getRoom().getTitle(),
				"VISITOR",
				request.getStatus().name()
			)));

		return new MyRoomListResponse(items, 0, items.size(), false);
	}

	@Transactional(readOnly = true)
	public OpenChatResponse getOpenChat(Long roomId, Long userId) {
		Room room = getRoomOrThrow(roomId);
		ViewerState viewerState = resolveViewerState(room, userId);
		if (!viewerState.role().equals("HOST") && !viewerState.joinStatus().equals("APPROVED")) {
			throw new ProjectException(GeneralErrorCode.FORBIDDEN);
		}
		return new OpenChatResponse(room.getOpenChatUrl(), room.getOpenChatPassword());
	}

	Room getRoomOrThrow(Long roomId) {
		return roomRepository.findById(roomId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	User getUserOrThrow(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	boolean isHost(Room room, Long userId) {
		return room.getHostUser().getId().equals(userId);
	}

	boolean isFull(Room room) {
		return roomMemberRepository.countByRoomId(room.getId()) >= room.getMaxMembers();
	}

	private RoomListItemResponse toRoomListItem(Room room, Set<InterestTag> userTags) {
		List<InterestTag> roomTags = roomTagRepository.findAllByRoomIdOrderByIdAsc(room.getId())
			.stream()
			.map(RoomTag::getTag)
			.toList();
		long memberCount = roomMemberRepository.countByRoomId(room.getId());
		int matchCount = (int) roomTags.stream()
			.filter(userTags::contains)
			.count();

		return new RoomListItemResponse(
			room.getId(),
			room.getTitle(),
			room.getHostUser().getNickname(),
			room.getStatus().name(),
			memberCount >= room.getMaxMembers(),
			memberCount,
			room.getMaxMembers(),
			RoomDateTimeFormatter.format(room.getMeetingAt()),
			room.getMeetingPlace().getName(),
			roomTags,
			matchCount
		);
	}

	private RoomListResponse page(List<RoomListItemResponse> allItems, int page, int size) {
		int fromIndex = Math.min(page * size, allItems.size());
		int toIndex = Math.min(fromIndex + size, allItems.size());
		return new RoomListResponse(
			allItems.subList(fromIndex, toIndex),
			page,
			size,
			toIndex < allItems.size()
		);
	}

	private ViewerState resolveViewerState(Room room, Long userId) {
		if (isHost(room, userId)) {
			return new ViewerState("HOST", "APPROVED");
		}
		if (roomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
			return new ViewerState("MEMBER", "APPROVED");
		}
		Optional<JoinRequest> joinRequest = joinRequestRepository.findByRoomIdAndUserId(room.getId(), userId);
		return joinRequest
			.map(request -> new ViewerState("VISITOR", request.getStatus().name()))
			.orElseGet(() -> new ViewerState("VISITOR", "NOT_REQUESTED"));
	}

	private Set<InterestTag> getUserInterestTags(Long userId, Long concertId) {
		return concertInterestTagRepository.findAllByUserIdAndConcertIdOrderByIdAsc(userId, concertId)
			.stream()
			.map(com.buddyduck.buddyduck.domain.concert.entity.ConcertInterestTag::getTag)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Place upsertPlace(RoomPlaceRequest request) {
		Place place = findReusablePlace(request)
			.map(existingPlace -> {
				existingPlace.update(request.name(), request.address(), request.lat(), request.lng());
				return existingPlace;
			})
			.orElseGet(() -> Place.create(
				request.provider(),
				request.providerPlaceId(),
				request.name(),
				request.address(),
				request.lat(),
				request.lng()
			));
		return placeRepository.save(place);
	}

	private Optional<Place> findReusablePlace(RoomPlaceRequest request) {
		if (StringUtils.hasText(request.providerPlaceId())) {
			return placeRepository.findByProviderAndProviderPlaceId(request.provider(), request.providerPlaceId());
		}
		return placeRepository.findFirstByProviderAndNameAndAddressOrderByIdAsc(
			request.provider(),
			request.name(),
			request.address()
		);
	}

	private Concert getConcertOrThrow(Long concertId) {
		return concertRepository.findById(concertId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	private RoomStatus parseRoomStatus(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return RoomStatus.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
	}

	private Set<InterestTag> parseTags(String value) {
		if (!StringUtils.hasText(value)) {
			return Set.of();
		}
		try {
			return List.of(value.split(","))
				.stream()
				.map(String::trim)
				.filter(StringUtils::hasText)
				.map(InterestTag::valueOf)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		} catch (IllegalArgumentException e) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
	}

	private List<InterestTag> deduplicate(List<InterestTag> tags) {
		return List.copyOf(new LinkedHashSet<>(tags));
	}

	private record ViewerState(String role, String joinStatus) {
	}
}
