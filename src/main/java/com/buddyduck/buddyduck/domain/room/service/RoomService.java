package com.buddyduck.buddyduck.domain.room.service;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.entity.ConcertInterestTag;
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
import com.buddyduck.buddyduck.domain.room.dto.RoomDetailConcertResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomDetailMemberResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomDetailResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomDetailScheduleSlotResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomLeaveResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomListItemResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomListResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomManagementResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomPermissionsResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomPlaceRequest;
import com.buddyduck.buddyduck.domain.room.dto.UpdateRoomRequest;
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
import com.buddyduck.buddyduck.domain.schedule.entity.RouteSegment;
import com.buddyduck.buddyduck.domain.schedule.entity.ScheduleSlot;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotCategory;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotType;
import com.buddyduck.buddyduck.domain.schedule.repository.RouteSegmentRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleSlotRepository;
import com.buddyduck.buddyduck.domain.schedule.route.FallbackRouteEstimator;
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimate;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomTagRepository roomTagRepository;
	private final JoinRequestRepository joinRequestRepository;
	private final ConcertRepository concertRepository;
	private final ConcertInterestTagRepository concertInterestTagRepository;
	private final PlaceRepository placeRepository;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final RouteSegmentRepository routeSegmentRepository;
	private final FallbackRouteEstimator fallbackRouteEstimator;
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
			request.meetingAt().atZoneSameInstant(KST).toLocalDateTime(),
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
		createDefaultScheduleSlots(schedule, room);

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
		boolean member = viewerState.role().equals("MEMBER");
		RoomPermissionsResponse permissions = new RoomPermissionsResponse(
			viewerState.role().equals("VISITOR") && viewerState.joinStatus().equals("NOT_REQUESTED") && !full && room.getStatus() == RoomStatus.OPEN,
			host,
			host || member,
			host || member,
			host
		);
		long pendingRequestCount = joinRequestRepository.countByRoomIdAndStatus(roomId, JoinRequestStatus.PENDING);
		List<InterestTag> roomTags = roomTagRepository.findAllByRoomIdOrderByIdAsc(roomId)
			.stream()
			.map(RoomTag::getTag)
			.toList();
		long memberCount = roomMemberRepository.countByRoomId(roomId);

		return new RoomDetailResponse(
			room.getId(),
			room.getTitle(),
			room.getDescription(),
			room.getStatus().name(),
			viewerState.role(),
			viewerState.joinStatus(),
			permissions,
			pendingRequestCount,
			new RoomDetailConcertResponse(
				room.getConcert().getId(),
				room.getConcert().getTitle(),
				RoomDateTimeFormatter.format(room.getConcert().getStartAt()),
				room.getConcert().getVenueName()
			),
			RoomDateTimeFormatter.format(room.getMeetingAt()),
			room.getMeetingPlace().getName(),
			room.getMeetingPlace().getAddress(),
			roomTags,
			memberCount,
			room.getMaxMembers(),
			toRoomDetailMembers(room, roomTags),
			toSchedulePreview(roomId)
		);
	}

	@Transactional(readOnly = true)
	public MyRoomListResponse getMyRooms(Long userId, String tab) {
		getUserOrThrow(userId);
		MyRoomTab roomTab = parseMyRoomTab(tab);
		LocalDateTime now = LocalDateTime.now(KST);
		List<MyRoomSource> sources = new ArrayList<>();

		if (roomTab.includes(MyRoomTab.HOSTED)) {
			roomRepository.findAllByHostUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.filter(room -> isVisibleMyRoom(room, now))
				.forEach(room -> sources.add(new MyRoomSource(room, "HOST", "APPROVED")));
		}

		if (roomTab.includes(MyRoomTab.JOINED)) {
			roomMemberRepository.findAllByUserIdOrderByJoinedAtDesc(userId)
				.stream()
				.filter(member -> member.getRole() == RoomMemberRole.MEMBER)
				.filter(member -> isVisibleMyRoom(member.getRoom(), now))
				.forEach(member -> sources.add(new MyRoomSource(member.getRoom(), "MEMBER", "APPROVED")));
		}

		if (roomTab.includes(MyRoomTab.PENDING)) {
			joinRequestRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.filter(request -> request.getStatus() == JoinRequestStatus.PENDING)
				.filter(request -> isVisibleMyRoom(request.getRoom(), now))
				.forEach(request -> sources.add(new MyRoomSource(request.getRoom(), "VISITOR", request.getStatus().name())));
		}

		Set<Long> roomIds = sources.stream()
			.map(source -> source.room().getId())
			.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Long> memberCounts = countMembersByRoomIds(roomIds);
		Map<Long, Long> pendingRequestCounts = countPendingRequestsByRoomIds(roomIds);
		List<MyRoomItemResponse> items = sources.stream()
			.map(source -> toMyRoomItem(
				source.room(),
				source.viewerRole(),
				source.viewerJoinStatus(),
				memberCounts.getOrDefault(source.room().getId(), 0L),
				pendingRequestCounts.getOrDefault(source.room().getId(), 0L)
			))
			.toList();

		return new MyRoomListResponse(items, 0, items.size(), false);
	}

	@Transactional(readOnly = true)
	public OpenChatResponse getOpenChat(Long roomId, Long userId) {
		Room room = getRoomOrThrow(roomId);
		ViewerState viewerState = resolveViewerState(room, userId);
		if (!viewerState.role().equals("HOST") && !viewerState.role().equals("MEMBER")) {
			throw new ProjectException(GeneralErrorCode.FORBIDDEN);
		}
		return new OpenChatResponse(room.getOpenChatUrl(), room.getOpenChatPassword());
	}

	@Transactional
	public RoomLeaveResponse leaveRoom(Long roomId, Long userId) {
		Room room = getRoomForUpdateOrThrow(roomId);
		if (isHost(room, userId)) {
			throw new ProjectException(GeneralErrorCode.FORBIDDEN);
		}
		RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

		roomMemberRepository.delete(member);
		joinRequestRepository.findByRoomIdAndUserId(roomId, userId)
			.ifPresent(joinRequestRepository::delete);
		if (room.getStatus() != RoomStatus.CLOSED && !isFull(room)) {
			room.markOpen();
		}

		return new RoomLeaveResponse("LEFT");
	}

	@Transactional
	public RoomManagementResponse updateRoom(Long roomId, Long userId, UpdateRoomRequest request) {
		Room room = getRoomForUpdateOrThrow(roomId);
		requireHost(room, userId);
		long memberCount = roomMemberRepository.countByRoomId(roomId);
		if (request.maxMembers() < 2 || request.maxMembers() < memberCount) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}

		Place meetingPlace = upsertPlace(request.meetingPlace());
		Place eventPlace = upsertPlace(request.eventPlace());
		room.update(
			request.title(),
			request.description(),
			request.maxMembers(),
			request.meetingAt().atZoneSameInstant(KST).toLocalDateTime(),
			meetingPlace,
			eventPlace,
			request.openChatUrl(),
			request.openChatPassword()
		);
		replaceRoomTags(room, request.roomTags());
		syncDefaultScheduleAnchors(room);
		if (room.getStatus() != RoomStatus.CLOSED) {
			updateCapacityStatus(room, memberCount);
		}

		return toRoomManagementResponse(room);
	}

	@Transactional
	public RoomManagementResponse closeRoom(Long roomId, Long userId) {
		Room room = closeRoomInternal(roomId, userId);
		return toRoomManagementResponse(room);
	}

	@Transactional
	public RoomManagementResponse deleteRoom(Long roomId, Long userId) {
		Room room = closeRoomInternal(roomId, userId);
		return toRoomManagementResponse(room);
	}

	public Room getRoomOrThrow(Long roomId) {
		return roomRepository.findById(roomId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	Room getRoomForUpdateOrThrow(Long roomId) {
		return roomRepository.findByIdForUpdate(roomId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	User getUserOrThrow(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	public boolean isHost(Room room, Long userId) {
		return room.getHostUser().getId().equals(userId);
	}

	boolean isFull(Room room) {
		return roomMemberRepository.countByRoomId(room.getId()) >= room.getMaxMembers();
	}

	private void requireHost(Room room, Long userId) {
		if (!isHost(room, userId)) {
			throw new ProjectException(GeneralErrorCode.FORBIDDEN);
		}
	}

	private RoomManagementResponse toRoomManagementResponse(Room room) {
		return new RoomManagementResponse(room.getId(), room.getStatus().name());
	}

	private Room closeRoomInternal(Long roomId, Long userId) {
		Room room = getRoomForUpdateOrThrow(roomId);
		requireHost(room, userId);
		room.markClosed();
		return room;
	}

	private void replaceRoomTags(Room room, List<InterestTag> tags) {
		roomTagRepository.deleteByRoomId(room.getId());
		roomTagRepository.saveAll(deduplicate(tags).stream()
			.map(tag -> RoomTag.create(room, tag))
			.toList());
	}

	private void updateCapacityStatus(Room room, long memberCount) {
		if (memberCount >= room.getMaxMembers()) {
			room.markFull();
			return;
		}
		room.markOpen();
	}

	private void syncDefaultScheduleAnchors(Room room) {
		scheduleRepository.findByRoomId(room.getId())
			.ifPresent(schedule -> scheduleSlotRepository.findAllByScheduleIdOrderBySortOrderAsc(schedule.getId())
				.forEach(slot -> {
					if (!Boolean.TRUE.equals(slot.getLocked())) {
						return;
					}
					if (slot.getSlotType() == SlotType.MEETING) {
						slot.updateAnchor(
							room.getMeetingPlace(),
							room.getMeetingPlace().getName(),
							room.getMeetingAt(),
							room.getMeetingAt()
						);
					}
					if (slot.getSlotType() == SlotType.CONCERT) {
						slot.updateAnchor(
							room.getEventPlace(),
							room.getConcert().getTitle(),
							room.getConcert().getStartAt(),
							room.getConcert().getEndAt() == null
								? room.getConcert().getStartAt()
								: room.getConcert().getEndAt()
						);
					}
				}));
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

	private MyRoomItemResponse toMyRoomItem(
		Room room,
		String viewerRole,
		String viewerJoinStatus,
		long memberCount,
		long roomPendingRequestCount
	) {
		long pendingJoinRequestCount = "HOST".equals(viewerRole)
			? roomPendingRequestCount
			: 0;

		return new MyRoomItemResponse(
			room.getId(),
			room.getTitle(),
			viewerRole,
			viewerJoinStatus,
			room.getStatus().name(),
			room.getConcert().getTitle(),
			RoomDateTimeFormatter.format(room.getConcert().getStartAt()),
			calculateDaysUntilConcert(room),
			room.getConcert().getVenueName(),
			RoomDateTimeFormatter.format(room.getMeetingAt()),
			room.getMeetingPlace().getName(),
			room.getMeetingPlace().getAddress(),
			memberCount,
			room.getMaxMembers(),
			pendingJoinRequestCount
		);
	}

	private Map<Long, Long> countMembersByRoomIds(Set<Long> roomIds) {
		if (roomIds.isEmpty()) {
			return Map.of();
		}
		return roomMemberRepository.countMembersByRoomIds(roomIds)
			.stream()
			.collect(Collectors.toMap(
				RoomMemberRepository.RoomMemberCount::getRoomId,
				RoomMemberRepository.RoomMemberCount::getMemberCount
			));
	}

	private Map<Long, Long> countPendingRequestsByRoomIds(Set<Long> roomIds) {
		if (roomIds.isEmpty()) {
			return Map.of();
		}
		return joinRequestRepository.countRequestsByRoomIdsAndStatus(roomIds, JoinRequestStatus.PENDING)
			.stream()
			.collect(Collectors.toMap(
				JoinRequestRepository.RoomPendingRequestCount::getRoomId,
				JoinRequestRepository.RoomPendingRequestCount::getPendingRequestCount
			));
	}

	private long calculateDaysUntilConcert(Room room) {
		LocalDate today = LocalDate.now(KST);
		LocalDate concertDate = room.getConcert().getStartAt().toLocalDate();
		return ChronoUnit.DAYS.between(today, concertDate);
	}

	private boolean isActiveConcertRoom(Room room, LocalDateTime now) {
		LocalDate concertDate = room.getConcert().getStartAt().toLocalDate();
		return !concertDate.isBefore(now.toLocalDate());
	}

	private boolean isVisibleMyRoom(Room room, LocalDateTime now) {
		return room.getStatus() != RoomStatus.CLOSED && isActiveConcertRoom(room, now);
	}

	private void createDefaultScheduleSlots(Schedule schedule, Room room) {
		ScheduleSlot meetingSlot = scheduleSlotRepository.save(ScheduleSlot.create(
			schedule,
			room.getMeetingPlace(),
			SlotType.MEETING,
			SlotCategory.MEETING,
			room.getMeetingPlace().getName(),
			1,
			room.getMeetingAt(),
			room.getMeetingAt(),
			0,
			true
		));
		ScheduleSlot concertSlot = scheduleSlotRepository.save(ScheduleSlot.create(
			schedule,
			room.getEventPlace(),
			SlotType.CONCERT,
			SlotCategory.CONCERT,
			room.getConcert().getTitle(),
			2,
			room.getConcert().getStartAt(),
			room.getConcert().getEndAt() == null ? room.getConcert().getStartAt() : room.getConcert().getEndAt(),
			0,
			true
		));
		createDefaultRouteSegment(schedule, meetingSlot, concertSlot);
	}

	private void createDefaultRouteSegment(Schedule schedule, ScheduleSlot meetingSlot, ScheduleSlot concertSlot) {
		RouteEstimate estimate = fallbackRouteEstimator.estimate(
			RouteMode.WALK,
			meetingSlot.getPlace(),
			concertSlot.getPlace()
		);
		routeSegmentRepository.save(RouteSegment.create(
			schedule,
			meetingSlot,
			concertSlot,
			RouteMode.WALK,
			estimate.distanceMeters(),
			estimate.durationMinutes(),
			estimate.taxiFareWon(),
			estimate.tollFareWon(),
			estimate.provider(),
			false
		));
	}

	private List<RoomDetailMemberResponse> toRoomDetailMembers(Room room, List<InterestTag> roomTags) {
		Set<InterestTag> roomTagSet = new LinkedHashSet<>(roomTags);
		List<RoomMember> members = roomMemberRepository.findAllByRoomIdOrderByJoinedAtAscIdAsc(room.getId());
		List<Long> memberUserIds = members.stream()
			.map(member -> member.getUser().getId())
			.toList();
		Map<Long, Set<InterestTag>> userTagsByUserId = getUserInterestTagsByUserIds(
			memberUserIds,
			room.getConcert().getId()
		);

		return members
			.stream()
			.map(member -> {
				User memberUser = member.getUser();
				Set<InterestTag> userTags = userTagsByUserId.getOrDefault(memberUser.getId(), Set.of());
				int sharedInterestCount = (int) userTags.stream()
					.filter(roomTagSet::contains)
					.count();
				return new RoomDetailMemberResponse(
					memberUser.getId(),
					memberUser.getNickname(),
					memberUser.getAgeRange(),
					memberUser.getGender(),
					member.getRole().name(),
					sharedInterestCount
				);
			})
			.toList();
	}

	private Map<Long, Set<InterestTag>> getUserInterestTagsByUserIds(List<Long> userIds, Long concertId) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return concertInterestTagRepository.findAllByUserIdInAndConcertId(userIds, concertId)
			.stream()
			.collect(Collectors.groupingBy(
				interestTag -> interestTag.getUser().getId(),
				Collectors.mapping(ConcertInterestTag::getTag, Collectors.toCollection(LinkedHashSet::new))
			));
	}

	private List<RoomDetailScheduleSlotResponse> toSchedulePreview(Long roomId) {
		return scheduleRepository.findByRoomId(roomId)
			.map(schedule -> scheduleSlotRepository.findAllByScheduleIdOrderBySortOrderAsc(schedule.getId())
				.stream()
				.map(this::toSchedulePreviewSlot)
				.toList())
			.orElseGet(List::of);
	}

	private RoomDetailScheduleSlotResponse toSchedulePreviewSlot(ScheduleSlot slot) {
		Place place = slot.getPlace();
		return new RoomDetailScheduleSlotResponse(
			slot.getId(),
			slot.getSortOrder(),
			slot.getTitle(),
			place == null ? null : place.getId(),
			place == null ? null : place.getName(),
			slot.getSlotType(),
			slot.getCategory(),
			RoomDateTimeFormatter.format(slot.getStartAt()),
			RoomDateTimeFormatter.format(slot.getEndAt()),
			slot.getDwellMinutes(),
			slot.getLocked()
		);
	}

	private RoomListResponse page(List<RoomListItemResponse> allItems, int page, int size) {
		int fromIndex = (int) Math.min((long) page * size, allItems.size());
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

	private MyRoomTab parseMyRoomTab(String value) {
		if (!StringUtils.hasText(value) || value.equalsIgnoreCase("all")) {
			return MyRoomTab.ALL;
		}
		try {
			return MyRoomTab.valueOf(value.trim().toUpperCase());
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

	private record MyRoomSource(Room room, String viewerRole, String viewerJoinStatus) {
	}

	private enum MyRoomTab {
		ALL,
		HOSTED,
		JOINED,
		PENDING;

		private boolean includes(MyRoomTab target) {
			return this == ALL || this == target;
		}
	}
}
