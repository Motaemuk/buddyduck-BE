package com.buddyduck.buddyduck.domain.schedule.service;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.repository.PlaceRepository;
import com.buddyduck.buddyduck.domain.room.entity.Room;
import com.buddyduck.buddyduck.domain.room.repository.RoomMemberRepository;
import com.buddyduck.buddyduck.domain.room.service.RoomService;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftRouteSegmentRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftRouteSegmentResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftScheduleRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftScheduleResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftSlotRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftSlotResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.MapBoundsResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.MapPointResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.ScheduleMapResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.TimelineResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.TimelineRoomResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.TimelineRouteSegmentResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.TimelineScheduleResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.TimelineSlotResponse;
import com.buddyduck.buddyduck.domain.schedule.entity.RouteSegment;
import com.buddyduck.buddyduck.domain.schedule.entity.Schedule;
import com.buddyduck.buddyduck.domain.schedule.entity.ScheduleSlot;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotCategory;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotType;
import com.buddyduck.buddyduck.domain.schedule.repository.RouteSegmentRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleSlotRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private static final String TIMEZONE = "Asia/Seoul";

	private final ScheduleRepository scheduleRepository;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final RouteSegmentRepository routeSegmentRepository;
	private final PlaceRepository placeRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomService roomService;

	@Transactional(readOnly = true)
	public TimelineResponse getTimeline(Long roomId, Long userId) {
		Room room = roomService.getRoomOrThrow(roomId);
		requireRoomAccess(room, userId);
		Schedule schedule = getScheduleByRoomOrThrow(roomId);
		return toTimeline(room, schedule);
	}

	@Transactional(readOnly = true)
	public ScheduleMapResponse getMap(Long roomId, Long userId) {
		TimelineResponse timeline = getTimeline(roomId, userId);
		return new ScheduleMapResponse(
			timeline.slots(),
			timeline.routeSegments(),
			toMapBounds(timeline.slots())
		);
	}

	@Transactional(readOnly = true)
	public DraftScheduleResponse recalculateDraft(Long scheduleId, Long userId, DraftScheduleRequest request) {
		Schedule schedule = getScheduleOrThrow(scheduleId);
		requireRoomAccess(schedule.getRoom(), userId);
		validateDraft(request);

		return new DraftScheduleResponse(
			"OK",
			0,
			request.slots().stream().map(DraftSlotResponse::from).toList(),
			request.routeSegments().stream().map(DraftRouteSegmentResponse::from).toList(),
			List.of()
		);
	}

	@Transactional
	public TimelineResponse commitDraft(Long scheduleId, Long userId, DraftScheduleRequest request) {
		Schedule schedule = getScheduleOrThrow(scheduleId);
		requireRoomAccess(schedule.getRoom(), userId);
		validateDraft(request);

		routeSegmentRepository.deleteAllByScheduleId(scheduleId);
		routeSegmentRepository.flush();
		scheduleSlotRepository.deleteAllByScheduleId(scheduleId);
		scheduleSlotRepository.flush();
		schedule.updateArrivalBufferMinutes(request.arrivalBufferMinutes());

		Map<String, ScheduleSlot> savedSlots = saveSlots(schedule, request);
		saveRouteSegments(schedule, request.routeSegments(), savedSlots);

		return toTimeline(schedule.getRoom(), schedule);
	}

	private Map<String, ScheduleSlot> saveSlots(Schedule schedule, DraftScheduleRequest request) {
		List<DraftSlotRequest> sortedSlots = request.slots().stream()
			.sorted(Comparator.comparing(DraftSlotRequest::order))
			.toList();
		Map<String, DraftRouteSegmentRequest> routeByToClientId = request.routeSegments().stream()
			.collect(LinkedHashMap::new, (map, route) -> map.put(route.toClientId(), route), Map::putAll);
		Map<String, ScheduleSlot> savedSlots = new LinkedHashMap<>();
		Map<Long, Place> placesById = findPlacesById(sortedSlots);
		LocalDateTime current = schedule.getRoom().getMeetingAt();

		for (DraftSlotRequest draftSlot : sortedSlots) {
			DraftRouteSegmentRequest incomingRoute = routeByToClientId.get(draftSlot.clientId());
			if (incomingRoute != null) {
				current = current.plusMinutes(incomingRoute.durationMinutes());
			}
			LocalDateTime startAt = current;
			LocalDateTime endAt = startAt.plusMinutes(draftSlot.dwellMinutes());
			Place place = draftSlot.placeId() == null ? null : placesById.get(draftSlot.placeId());
			ScheduleSlot savedSlot = scheduleSlotRepository.save(ScheduleSlot.create(
				schedule,
				place,
				draftSlot.slotType() == null ? SlotType.PLACE : draftSlot.slotType(),
				draftSlot.category() == null ? SlotCategory.ETC : draftSlot.category(),
				draftSlot.title(),
				draftSlot.order(),
				startAt,
				endAt,
				draftSlot.dwellMinutes(),
				Boolean.TRUE.equals(draftSlot.locked())
			));
			savedSlots.put(draftSlot.clientId(), savedSlot);
			current = endAt;
		}

		return savedSlots;
	}

	private void saveRouteSegments(
		Schedule schedule,
		List<DraftRouteSegmentRequest> draftRouteSegments,
		Map<String, ScheduleSlot> savedSlots
	) {
		List<RouteSegment> routeSegments = draftRouteSegments.stream()
			.map(draftRoute -> RouteSegment.create(
				schedule,
				requiredSlot(savedSlots, draftRoute.fromClientId()),
				requiredSlot(savedSlots, draftRoute.toClientId()),
				draftRoute.mode(),
				null,
				draftRoute.durationMinutes(),
				"MANUAL",
				true
			))
			.toList();
		routeSegmentRepository.saveAll(routeSegments);
	}

	private TimelineResponse toTimeline(Room room, Schedule schedule) {
		List<TimelineSlotResponse> slots = scheduleSlotRepository.findAllByScheduleIdOrderBySortOrderAsc(schedule.getId())
			.stream()
			.map(TimelineSlotResponse::from)
			.toList();
		List<TimelineRouteSegmentResponse> routeSegments = routeSegmentRepository
			.findAllByScheduleIdOrderByIdAsc(schedule.getId())
			.stream()
			.map(TimelineRouteSegmentResponse::from)
			.toList();

		return new TimelineResponse(
			new TimelineRoomResponse(room.getId(), room.getTitle()),
			new TimelineScheduleResponse(schedule.getId(), schedule.getArrivalBufferMinutes(), TIMEZONE),
			slots,
			routeSegments,
			List.of()
		);
	}

	private MapBoundsResponse toMapBounds(List<TimelineSlotResponse> slots) {
		List<TimelineSlotResponse> slotsWithCoordinates = slots.stream()
			.filter(slot -> slot.lat() != null && slot.lng() != null)
			.toList();
		if (slotsWithCoordinates.isEmpty()) {
			BigDecimal zero = BigDecimal.ZERO;
			return new MapBoundsResponse(new MapPointResponse(zero, zero), new MapPointResponse(zero, zero));
		}

		BigDecimal minLat = slotsWithCoordinates.stream().map(TimelineSlotResponse::lat).min(BigDecimal::compareTo).orElseThrow();
		BigDecimal minLng = slotsWithCoordinates.stream().map(TimelineSlotResponse::lng).min(BigDecimal::compareTo).orElseThrow();
		BigDecimal maxLat = slotsWithCoordinates.stream().map(TimelineSlotResponse::lat).max(BigDecimal::compareTo).orElseThrow();
		BigDecimal maxLng = slotsWithCoordinates.stream().map(TimelineSlotResponse::lng).max(BigDecimal::compareTo).orElseThrow();

		return new MapBoundsResponse(
			new MapPointResponse(minLat, minLng),
			new MapPointResponse(maxLat, maxLng)
		);
	}

	private void requireRoomAccess(Room room, Long userId) {
		if (!roomService.isHost(room, userId) && !roomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
			throw new ProjectException(GeneralErrorCode.FORBIDDEN);
		}
	}

	private Schedule getScheduleByRoomOrThrow(Long roomId) {
		return scheduleRepository.findByRoomId(roomId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	private Schedule getScheduleOrThrow(Long scheduleId) {
		return scheduleRepository.findById(scheduleId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	private Map<Long, Place> findPlacesById(List<DraftSlotRequest> draftSlots) {
		Set<Long> placeIds = draftSlots.stream()
			.map(DraftSlotRequest::placeId)
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		Map<Long, Place> placesById = new LinkedHashMap<>();
		placeRepository.findAllById(placeIds)
			.forEach(place -> placesById.put(place.getId(), place));
		if (placesById.size() != placeIds.size()) {
			throw new ProjectException(GeneralErrorCode.NOT_FOUND);
		}
		return placesById;
	}

	private ScheduleSlot requiredSlot(Map<String, ScheduleSlot> slots, String clientId) {
		ScheduleSlot slot = slots.get(clientId);
		if (slot == null) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		return slot;
	}

	private void validateDraft(DraftScheduleRequest request) {
		Set<String> slotClientIds = new HashSet<>();
		Set<Integer> slotOrders = new HashSet<>();
		for (DraftSlotRequest slot : request.slots()) {
			if (!slotClientIds.add(slot.clientId()) || !slotOrders.add(slot.order())) {
				throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
			}
		}

		Set<String> routeToClientIds = new HashSet<>();
		for (DraftRouteSegmentRequest route : request.routeSegments()) {
			if (Objects.equals(route.fromClientId(), route.toClientId())
				|| !slotClientIds.contains(route.fromClientId())
				|| !slotClientIds.contains(route.toClientId())
				|| !routeToClientIds.add(route.toClientId())) {
				throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
			}
		}
	}
}
