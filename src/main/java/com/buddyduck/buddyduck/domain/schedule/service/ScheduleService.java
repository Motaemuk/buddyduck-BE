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
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimate;
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimator;
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
import org.springframework.transaction.support.TransactionTemplate;

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
	private final RouteEstimator routeEstimator;
	private final TransactionTemplate transactionTemplate;

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

	public DraftScheduleResponse recalculateDraft(Long scheduleId, Long userId, DraftScheduleRequest request) {
		DraftCalculationContext context = loadDraftCalculationContext(scheduleId, userId, request);
		List<ResolvedDraftRouteSegment> resolvedRouteSegments = resolveRouteSegments(
			request.routeSegments(),
			context.draftSlotsByClientId(),
			context.placesById()
		);

		return new DraftScheduleResponse(
			"OK",
			0,
			request.slots().stream().map(DraftSlotResponse::from).toList(),
			resolvedRouteSegments.stream().map(ResolvedDraftRouteSegment::toResponse).toList(),
			List.of()
		);
	}

	public TimelineResponse commitDraft(Long scheduleId, Long userId, DraftScheduleRequest request) {
		DraftCalculationContext context = loadDraftCalculationContext(scheduleId, userId, request);
		List<ResolvedDraftRouteSegment> resolvedRouteSegments = resolveRouteSegments(
			request.routeSegments(),
			context.draftSlotsByClientId(),
			context.placesById()
		);

		return Objects.requireNonNull(transactionTemplate.execute(status -> saveDraft(
			scheduleId,
			userId,
			request,
			resolvedRouteSegments
		)));
	}

	private DraftCalculationContext loadDraftCalculationContext(
		Long scheduleId,
		Long userId,
		DraftScheduleRequest request
	) {
		return Objects.requireNonNull(transactionTemplate.execute(status -> {
			Schedule schedule = getScheduleOrThrow(scheduleId);
			requireRoomAccess(schedule.getRoom(), userId);
			validateDraft(request);
			return new DraftCalculationContext(
				draftSlotsByClientId(request),
				findPlacesById(request.slots())
			);
		}));
	}

	private TimelineResponse saveDraft(
		Long scheduleId,
		Long userId,
		DraftScheduleRequest request,
		List<ResolvedDraftRouteSegment> resolvedRouteSegments
	) {
		Schedule schedule = getScheduleOrThrow(scheduleId);
		requireRoomAccess(schedule.getRoom(), userId);
		validateDraft(request);
		Map<Long, Place> placesById = findPlacesById(request.slots());

		routeSegmentRepository.deleteAllByScheduleId(scheduleId);
		routeSegmentRepository.flush();
		scheduleSlotRepository.deleteAllByScheduleId(scheduleId);
		scheduleSlotRepository.flush();
		schedule.updateArrivalBufferMinutes(request.arrivalBufferMinutes());

		Map<String, ScheduleSlot> savedSlots = saveSlots(schedule, request, placesById, resolvedRouteSegments);
		saveRouteSegments(schedule, resolvedRouteSegments, savedSlots);

		return toTimeline(schedule.getRoom(), schedule);
	}

	private Map<String, ScheduleSlot> saveSlots(
		Schedule schedule,
		DraftScheduleRequest request,
		Map<Long, Place> placesById,
		List<ResolvedDraftRouteSegment> resolvedRouteSegments
	) {
		List<DraftSlotRequest> sortedSlots = request.slots().stream()
			.sorted(Comparator.comparing(DraftSlotRequest::order))
			.toList();
		Map<String, Integer> routeDurationByToClientId = resolvedRouteSegments.stream()
			.collect(
				LinkedHashMap::new,
				(map, route) -> map.put(route.request().toClientId(), route.estimate().durationMinutes()),
				Map::putAll
			);
		Map<String, ScheduleSlot> savedSlots = new LinkedHashMap<>();
		LocalDateTime current = schedule.getRoom().getMeetingAt();

		for (DraftSlotRequest draftSlot : sortedSlots) {
			Integer incomingDuration = routeDurationByToClientId.get(draftSlot.clientId());
			if (incomingDuration != null) {
				current = current.plusMinutes(incomingDuration);
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
		List<ResolvedDraftRouteSegment> resolvedRouteSegments,
		Map<String, ScheduleSlot> savedSlots
	) {
		List<RouteSegment> routeSegments = resolvedRouteSegments.stream()
			.map(resolvedRoute -> RouteSegment.create(
				schedule,
				requiredSlot(savedSlots, resolvedRoute.request().fromClientId()),
				requiredSlot(savedSlots, resolvedRoute.request().toClientId()),
				resolvedRoute.estimate().mode(),
				resolvedRoute.estimate().distanceMeters(),
				resolvedRoute.estimate().durationMinutes(),
				resolvedRoute.estimate().taxiFareWon(),
				resolvedRoute.estimate().tollFareWon(),
				resolvedRoute.estimate().provider(),
				resolvedRoute.estimate().manuallyAdjusted()
			))
			.toList();
		routeSegmentRepository.saveAll(routeSegments);
	}

	private Map<String, DraftSlotRequest> draftSlotsByClientId(DraftScheduleRequest request) {
		return request.slots().stream()
			.collect(LinkedHashMap::new, (map, slot) -> map.put(slot.clientId(), slot), Map::putAll);
	}

	private List<ResolvedDraftRouteSegment> resolveRouteSegments(
		List<DraftRouteSegmentRequest> draftRouteSegments,
		Map<String, DraftSlotRequest> draftSlotsByClientId,
		Map<Long, Place> placesById
	) {
		return draftRouteSegments.stream()
			.map(route -> new ResolvedDraftRouteSegment(
				route,
				resolveRouteEstimate(route, draftSlotsByClientId, placesById)
			))
			.toList();
	}

	private RouteEstimate resolveRouteEstimate(
		DraftRouteSegmentRequest route,
		Map<String, DraftSlotRequest> draftSlotsByClientId,
		Map<Long, Place> placesById
	) {
		if (Boolean.TRUE.equals(route.manuallyAdjusted())) {
			return RouteEstimate.manual(route.mode(), route.durationMinutes());
		}

		DraftSlotRequest fromSlot = draftSlotsByClientId.get(route.fromClientId());
		DraftSlotRequest toSlot = draftSlotsByClientId.get(route.toClientId());
		Place fromPlace = placeOrNull(fromSlot, placesById);
		Place toPlace = placeOrNull(toSlot, placesById);
		if (fromPlace == null || toPlace == null) {
			return RouteEstimate.unresolvedPlace(route.mode(), route.durationMinutes());
		}
		return routeEstimator.estimate(route.mode(), fromPlace, toPlace);
	}

	private Place placeOrNull(DraftSlotRequest draftSlot, Map<Long, Place> placesById) {
		if (draftSlot == null || draftSlot.placeId() == null) {
			return null;
		}
		return placesById.get(draftSlot.placeId());
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

	private record ResolvedDraftRouteSegment(
		DraftRouteSegmentRequest request,
		RouteEstimate estimate
	) {

		private DraftRouteSegmentResponse toResponse() {
			return DraftRouteSegmentResponse.from(request, estimate);
		}
	}

	private record DraftCalculationContext(
		Map<String, DraftSlotRequest> draftSlotsByClientId,
		Map<Long, Place> placesById
	) {
	}
}
