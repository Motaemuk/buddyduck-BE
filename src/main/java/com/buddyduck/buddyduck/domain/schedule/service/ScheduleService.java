package com.buddyduck.buddyduck.domain.schedule.service;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.repository.PlaceRepository;
import com.buddyduck.buddyduck.domain.room.entity.Room;
import com.buddyduck.buddyduck.domain.room.repository.RoomMemberRepository;
import com.buddyduck.buddyduck.domain.room.service.RoomService;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftRouteSegmentRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftRouteSegmentResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftScheduleRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftScheduleRecommendationRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftScheduleResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftSlotRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftSlotResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.MapBoundsResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.MapPointResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.ScheduleDateTimeFormatter;
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
import com.buddyduck.buddyduck.domain.schedule.exception.ScheduleErrorCode;
import com.buddyduck.buddyduck.domain.schedule.repository.RouteSegmentRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleSlotRepository;
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimate;
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimator;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
	private static final ZoneId SEOUL_ZONE = ZoneId.of(TIMEZONE);

	private final ScheduleRepository scheduleRepository;
	private final ScheduleSlotRepository scheduleSlotRepository;
	private final RouteSegmentRepository routeSegmentRepository;
	private final PlaceRepository placeRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomService roomService;
	private final RouteEstimator routeEstimator;
	private final ScheduleRouteRecommendationService scheduleRouteRecommendationService;
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
		DraftTimeline draftTimeline = buildDraftTimeline(context.schedule(), request, resolvedRouteSegments);

		return toDraftScheduleResponse(draftTimeline, resolvedRouteSegments);
	}

	public DraftScheduleResponse recommendDraft(
		Long scheduleId,
		Long userId,
		DraftScheduleRecommendationRequest request
	) {
		DraftRecommendationContext context = loadDraftRecommendationContext(scheduleId, userId, request);
		ScheduleRouteRecommendationService.RouteRecommendation recommendation = scheduleRouteRecommendationService.recommend(
			request.slots(),
			context.placesById(),
			request.recommendationMode()
		);
		DraftScheduleRequest draftRequest = new DraftScheduleRequest(
			request.arrivalBufferMinutes(),
			request.customStartAt(),
			request.targetArrivalAt(),
			recommendation.slots(),
			recommendation.routeSegments()
		);
		List<ResolvedDraftRouteSegment> resolvedRouteSegments = resolveRouteSegments(
			draftRequest.routeSegments(),
			draftSlotsByClientId(draftRequest),
			context.placesById()
		);
		DraftTimeline draftTimeline = buildDraftTimeline(context.schedule(), draftRequest, resolvedRouteSegments);

		return toDraftScheduleResponse(draftTimeline, resolvedRouteSegments);
	}

	private DraftScheduleResponse toDraftScheduleResponse(
		DraftTimeline draftTimeline,
		List<ResolvedDraftRouteSegment> resolvedRouteSegments
	) {
		return new DraftScheduleResponse(
			draftTimeline.fitStatus(),
			ScheduleDateTimeFormatter.format(draftTimeline.recommendedStartAt()),
			ScheduleDateTimeFormatter.format(draftTimeline.effectiveStartAt()),
			ScheduleDateTimeFormatter.format(draftTimeline.targetArrivalAt()),
			draftTimeline.overrunMinutes(),
			draftTimeline.spareMinutes(),
			draftTimeline.slots().stream().map(ResolvedDraftSlot::toResponse).toList(),
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
		DraftTimeline draftTimeline = buildDraftTimeline(context.schedule(), request, resolvedRouteSegments);
		validateCommittable(draftTimeline, resolvedRouteSegments);

		return Objects.requireNonNull(transactionTemplate.execute(status -> saveDraft(
			scheduleId,
			userId,
			request,
			draftTimeline,
			resolvedRouteSegments
		)));
	}

	private void validateCommittable(
		DraftTimeline draftTimeline,
		List<ResolvedDraftRouteSegment> resolvedRouteSegments
	) {
		if (draftTimeline.overrunMinutes() > 0) {
			throw new ProjectException(
				ScheduleErrorCode.SCHEDULE_OVERRUN,
				toDraftScheduleResponse(draftTimeline, resolvedRouteSegments)
			);
		}
	}

	private DraftCalculationContext loadDraftCalculationContext(
		Long scheduleId,
		Long userId,
		DraftScheduleRequest request
	) {
		return Objects.requireNonNull(transactionTemplate.execute(status -> {
			Schedule schedule = getScheduleWithRoomAndConcertOrThrow(scheduleId);
			requireRoomAccess(schedule.getRoom(), userId);
			validateDraft(request);
			return new DraftCalculationContext(
				schedule,
				draftSlotsByClientId(request),
				findPlacesById(request.slots())
			);
		}));
	}

	private DraftRecommendationContext loadDraftRecommendationContext(
		Long scheduleId,
		Long userId,
		DraftScheduleRecommendationRequest request
	) {
		return Objects.requireNonNull(transactionTemplate.execute(status -> {
			Schedule schedule = getScheduleWithRoomAndConcertOrThrow(scheduleId);
			requireRoomAccess(schedule.getRoom(), userId);
			validateDraftSlots(request.slots());
			Map<Long, Place> placesById = findPlacesById(request.slots());
			return new DraftRecommendationContext(schedule, placesById);
		}));
	}

	private TimelineResponse saveDraft(
		Long scheduleId,
		Long userId,
		DraftScheduleRequest request,
		DraftTimeline draftTimeline,
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
		schedule.updatePlanningTimes(
			request.arrivalBufferMinutes(),
			customStartAtToPersist(schedule, request),
			targetArrivalAtToPersist(schedule, request)
		);

		Map<String, ScheduleSlot> savedSlots = saveSlots(schedule, placesById, draftTimeline.slots());
		saveRouteSegments(schedule, resolvedRouteSegments, savedSlots);

		return toTimeline(schedule.getRoom(), schedule);
	}

	private Map<String, ScheduleSlot> saveSlots(
		Schedule schedule,
		Map<Long, Place> placesById,
		List<ResolvedDraftSlot> resolvedSlots
	) {
		Map<String, ScheduleSlot> savedSlots = new LinkedHashMap<>();
		for (ResolvedDraftSlot resolvedSlot : resolvedSlots) {
			DraftSlotRequest draftSlot = resolvedSlot.request();
			Place place = draftSlot.placeId() == null ? null : placesById.get(draftSlot.placeId());
			ScheduleSlot savedSlot = scheduleSlotRepository.save(ScheduleSlot.create(
				schedule,
				place,
				draftSlot.slotType() == null ? SlotType.PLACE : draftSlot.slotType(),
				draftSlot.category() == null ? SlotCategory.ETC : draftSlot.category(),
				draftSlot.title(),
				draftSlot.order(),
				resolvedSlot.startAt(),
				resolvedSlot.endAt(),
				draftSlot.dwellMinutes(),
				Boolean.TRUE.equals(draftSlot.locked())
			));
			savedSlots.put(draftSlot.clientId(), savedSlot);
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

	private DraftTimeline buildDraftTimeline(
		Schedule schedule,
		DraftScheduleRequest request,
		List<ResolvedDraftRouteSegment> resolvedRouteSegments
	) {
		LocalDateTime targetArrivalAt = resolveTargetArrivalAt(schedule, request);
		Integer totalDurationMinutes = totalDurationMinutes(request, resolvedRouteSegments);
		LocalDateTime recommendedStartAt = targetArrivalAt.minusMinutes(totalDurationMinutes);
		LocalDateTime effectiveStartAt = resolveEffectiveStartAt(schedule, request);
		List<ResolvedDraftSlot> slots = resolveDraftSlots(request, resolvedRouteSegments, effectiveStartAt);
		LocalDateTime endAt = slots.isEmpty() ? effectiveStartAt : slots.get(slots.size() - 1).endAt();
		Long spare = Duration.between(endAt, targetArrivalAt).toMinutes();
		Integer spareMinutes = Math.toIntExact(Math.max(0, spare));
		Integer overrunMinutes = Math.toIntExact(Math.max(0, -spare));

		return new DraftTimeline(
			overrunMinutes > 0 ? "OVERRUN" : "OK",
			recommendedStartAt,
			effectiveStartAt,
			targetArrivalAt,
			overrunMinutes,
			spareMinutes,
			slots
		);
	}

	private List<ResolvedDraftSlot> resolveDraftSlots(
		DraftScheduleRequest request,
		List<ResolvedDraftRouteSegment> resolvedRouteSegments,
		LocalDateTime effectiveStartAt
	) {
		List<DraftSlotRequest> sortedSlots = request.slots().stream()
			.sorted(Comparator.comparing(DraftSlotRequest::order))
			.toList();
		Map<RouteEdge, Integer> routeDurationByEdge = routeDurationByEdge(resolvedRouteSegments);
		List<ResolvedDraftSlot> resolvedSlots = new ArrayList<>();
		LocalDateTime current = effectiveStartAt;

		for (int index = 0; index < sortedSlots.size(); index++) {
			DraftSlotRequest draftSlot = sortedSlots.get(index);
			if (index > 0) {
				RouteEdge edge = new RouteEdge(sortedSlots.get(index - 1).clientId(), draftSlot.clientId());
				Integer incomingDuration = routeDurationByEdge.get(edge);
				if (incomingDuration == null) {
					throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
				}
				current = current.plusMinutes(incomingDuration);
			}
			LocalDateTime startAt = current;
			LocalDateTime endAt = startAt.plusMinutes(draftSlot.dwellMinutes());
			resolvedSlots.add(new ResolvedDraftSlot(draftSlot, startAt, endAt));
			current = endAt;
		}

		return resolvedSlots;
	}

	private Integer totalDurationMinutes(
		DraftScheduleRequest request,
		List<ResolvedDraftRouteSegment> resolvedRouteSegments
	) {
		Integer dwellMinutes = request.slots().stream()
			.map(DraftSlotRequest::dwellMinutes)
			.reduce(0, Integer::sum);
		Integer routeMinutes = resolvedRouteSegments.stream()
			.map(route -> route.estimate().durationMinutes())
			.reduce(0, Integer::sum);
		return dwellMinutes + routeMinutes;
	}

	private Map<RouteEdge, Integer> routeDurationByEdge(List<ResolvedDraftRouteSegment> resolvedRouteSegments) {
		return resolvedRouteSegments.stream()
			.collect(
				LinkedHashMap::new,
				(map, route) -> map.put(
					new RouteEdge(route.request().fromClientId(), route.request().toClientId()),
					route.estimate().durationMinutes()
				),
				Map::putAll
			);
	}

	private LocalDateTime customStartAtToPersist(Schedule schedule, DraftScheduleRequest request) {
		if (request.customStartAt() != null) {
			return toSeoulLocalDateTime(request.customStartAt());
		}
		return schedule.getCustomStartAt();
	}

	private LocalDateTime targetArrivalAtToPersist(Schedule schedule, DraftScheduleRequest request) {
		if (request.targetArrivalAt() != null) {
			return toSeoulLocalDateTime(request.targetArrivalAt());
		}
		return schedule.getTargetArrivalAt();
	}

	private LocalDateTime resolveEffectiveStartAt(Schedule schedule, DraftScheduleRequest request) {
		if (request.customStartAt() != null) {
			return toSeoulLocalDateTime(request.customStartAt());
		}
		if (schedule.getCustomStartAt() != null) {
			return schedule.getCustomStartAt();
		}
		return schedule.getRoom().getMeetingAt();
	}

	private LocalDateTime resolveTargetArrivalAt(Schedule schedule, DraftScheduleRequest request) {
		if (request.targetArrivalAt() != null) {
			return toSeoulLocalDateTime(request.targetArrivalAt());
		}
		if (schedule.getTargetArrivalAt() != null) {
			return schedule.getTargetArrivalAt();
		}
		return schedule.getRoom().getConcert().getStartAt().minusMinutes(request.arrivalBufferMinutes());
	}

	private LocalDateTime toSeoulLocalDateTime(OffsetDateTime value) {
		return value.atZoneSameInstant(SEOUL_ZONE).toLocalDateTime();
	}

	private Place placeOrNull(DraftSlotRequest draftSlot, Map<Long, Place> placesById) {
		if (draftSlot == null || draftSlot.placeId() == null) {
			return null;
		}
		return placesById.get(draftSlot.placeId());
	}

	private TimelineResponse toTimeline(Room room, Schedule schedule) {
		List<ScheduleSlot> savedSlots = scheduleSlotRepository.findAllByScheduleIdOrderBySortOrderAsc(schedule.getId());
		List<RouteSegment> savedRouteSegments = routeSegmentRepository.findAllByScheduleIdOrderByIdAsc(schedule.getId());
		List<TimelineSlotResponse> slots = savedSlots.stream().map(TimelineSlotResponse::from).toList();
		List<TimelineRouteSegmentResponse> routeSegments = savedRouteSegments.stream()
			.map(TimelineRouteSegmentResponse::from)
			.toList();
		TimelinePlanningSummary planningSummary = summarizeTimeline(schedule, savedSlots, savedRouteSegments);

		return new TimelineResponse(
			new TimelineRoomResponse(room.getId(), room.getTitle()),
			new TimelineScheduleResponse(
				schedule.getId(),
				schedule.getArrivalBufferMinutes(),
				TIMEZONE,
				ScheduleDateTimeFormatter.format(schedule.getCustomStartAt()),
				ScheduleDateTimeFormatter.format(planningSummary.targetArrivalAt()),
				ScheduleDateTimeFormatter.format(planningSummary.recommendedStartAt()),
				planningSummary.overrunMinutes(),
				planningSummary.spareMinutes()
			),
			slots,
			routeSegments,
			List.of()
		);
	}

	private TimelinePlanningSummary summarizeTimeline(
		Schedule schedule,
		List<ScheduleSlot> slots,
		List<RouteSegment> routeSegments
	) {
		LocalDateTime targetArrivalAt = schedule.getTargetArrivalAt() == null
			? schedule.getRoom().getConcert().getStartAt().minusMinutes(schedule.getArrivalBufferMinutes())
			: schedule.getTargetArrivalAt();
		Integer totalDurationMinutes = slots.stream()
			.map(ScheduleSlot::getDwellMinutes)
			.reduce(0, Integer::sum)
			+ routeSegments.stream()
			.map(RouteSegment::getDurationMinutes)
			.reduce(0, Integer::sum);
		LocalDateTime recommendedStartAt = targetArrivalAt.minusMinutes(totalDurationMinutes);
		LocalDateTime effectiveStartAt = schedule.getCustomStartAt() == null
			? slots.stream().findFirst().map(ScheduleSlot::getStartAt).orElse(schedule.getRoom().getMeetingAt())
			: schedule.getCustomStartAt();
		LocalDateTime endAt = slots.isEmpty()
			? effectiveStartAt
			: slots.get(slots.size() - 1).getEndAt();
		Long spare = Duration.between(endAt, targetArrivalAt).toMinutes();

		return new TimelinePlanningSummary(
			recommendedStartAt,
			effectiveStartAt,
			targetArrivalAt,
			Math.toIntExact(Math.max(0, -spare)),
			Math.toIntExact(Math.max(0, spare))
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

	private Schedule getScheduleWithRoomAndConcertOrThrow(Long scheduleId) {
		return scheduleRepository.findByIdWithRoomAndConcert(scheduleId)
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
		validateDraftSlots(request.slots());
		Set<String> slotClientIds = request.slots().stream()
			.map(DraftSlotRequest::clientId)
			.collect(Collectors.toSet());

		List<DraftSlotRequest> sortedSlots = request.slots().stream()
			.sorted(Comparator.comparing(DraftSlotRequest::order))
			.toList();
		if (request.routeSegments().size() != Math.max(0, sortedSlots.size() - 1)) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}

		Set<RouteEdge> routeEdges = new HashSet<>();
		for (DraftRouteSegmentRequest route : request.routeSegments()) {
			RouteEdge routeEdge = new RouteEdge(route.fromClientId(), route.toClientId());
			if (Objects.equals(route.fromClientId(), route.toClientId())
				|| !slotClientIds.contains(route.fromClientId())
				|| !slotClientIds.contains(route.toClientId())
				|| !routeEdges.add(routeEdge)) {
				throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
			}
		}

		for (int index = 1; index < sortedSlots.size(); index++) {
			RouteEdge expectedEdge = new RouteEdge(
				sortedSlots.get(index - 1).clientId(),
				sortedSlots.get(index).clientId()
			);
			if (!routeEdges.contains(expectedEdge)) {
				throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
			}
		}
	}

	private void validateDraftSlots(List<DraftSlotRequest> slots) {
		if (slots == null) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		Set<String> slotClientIds = new HashSet<>();
		Set<Integer> slotOrders = new HashSet<>();
		for (DraftSlotRequest slot : slots) {
			if (slot == null || slot.clientId() == null || slot.order() == null) {
				throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
			}
			if (!slotClientIds.add(slot.clientId()) || !slotOrders.add(slot.order())) {
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

	private record ResolvedDraftSlot(
		DraftSlotRequest request,
		LocalDateTime startAt,
		LocalDateTime endAt
	) {

		private DraftSlotResponse toResponse() {
			return DraftSlotResponse.from(request, startAt, endAt);
		}
	}

	private record DraftTimeline(
		String fitStatus,
		LocalDateTime recommendedStartAt,
		LocalDateTime effectiveStartAt,
		LocalDateTime targetArrivalAt,
		Integer overrunMinutes,
		Integer spareMinutes,
		List<ResolvedDraftSlot> slots
	) {
	}

	private record TimelinePlanningSummary(
		LocalDateTime recommendedStartAt,
		LocalDateTime effectiveStartAt,
		LocalDateTime targetArrivalAt,
		Integer overrunMinutes,
		Integer spareMinutes
	) {
	}

	private record RouteEdge(
		String fromClientId,
		String toClientId
	) {
	}

	private record DraftCalculationContext(
		Schedule schedule,
		Map<String, DraftSlotRequest> draftSlotsByClientId,
		Map<Long, Place> placesById
	) {
	}

	private record DraftRecommendationContext(
		Schedule schedule,
		Map<Long, Place> placesById
	) {
	}
}
