package com.buddyduck.buddyduck.domain.schedule.service;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftRouteSegmentRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftSlotRequest;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimate;
import com.buddyduck.buddyduck.domain.schedule.route.RouteEstimator;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleRouteRecommendationService {

	private final RouteEstimator routeEstimator;

	public RouteRecommendation recommend(
		List<DraftSlotRequest> slots,
		Map<Long, Place> placesById,
		RouteMode recommendationMode
	) {
		requireRecommendablePlaces(slots, placesById);
		List<DraftSlotRequest> recommendedSlots = recommendSlotOrder(slots, placesById, recommendationMode);
		return new RouteRecommendation(
			recommendedSlots,
			buildRecommendedRouteSegments(recommendedSlots, recommendationMode)
		);
	}

	private List<DraftSlotRequest> recommendSlotOrder(
		List<DraftSlotRequest> slots,
		Map<Long, Place> placesById,
		RouteMode mode
	) {
		List<DraftSlotRequest> sortedSlots = slots.stream()
			.sorted(Comparator.comparing(DraftSlotRequest::order))
			.toList();
		List<Integer> movableIndexes = movableIndexes(sortedSlots);
		if (movableIndexes.size() <= 1) {
			return withSequentialOrders(sortedSlots);
		}

		Map<RouteEdge, RouteEstimate> estimateCache = new LinkedHashMap<>();
		List<DraftSlotRequest> recommendedSlots = movableIndexes.size() <= 7
			? recommendByExhaustiveSearch(sortedSlots, movableIndexes, placesById, mode, estimateCache)
			: recommendByNearestNeighbor(sortedSlots, movableIndexes, placesById, mode, estimateCache);
		return withSequentialOrders(recommendedSlots);
	}

	private List<Integer> movableIndexes(List<DraftSlotRequest> sortedSlots) {
		List<Integer> indexes = new ArrayList<>();
		for (int index = 0; index < sortedSlots.size(); index++) {
			if (index == 0 || Boolean.TRUE.equals(sortedSlots.get(index).locked())) {
				continue;
			}
			indexes.add(index);
		}
		return indexes;
	}

	private List<DraftSlotRequest> recommendByExhaustiveSearch(
		List<DraftSlotRequest> sortedSlots,
		List<Integer> movableIndexes,
		Map<Long, Place> placesById,
		RouteMode mode,
		Map<RouteEdge, RouteEstimate> estimateCache
	) {
		List<DraftSlotRequest> movableSlots = movableIndexes.stream()
			.map(sortedSlots::get)
			.collect(Collectors.toCollection(ArrayList::new));
		RecommendedOrder bestOrder = searchBestOrder(
			sortedSlots,
			movableIndexes,
			movableSlots,
			0,
			placesById,
			mode,
			estimateCache,
			new RecommendedOrder(sortedSlots, Integer.MAX_VALUE)
		);
		return bestOrder.slots();
	}

	private RecommendedOrder searchBestOrder(
		List<DraftSlotRequest> sortedSlots,
		List<Integer> movableIndexes,
		List<DraftSlotRequest> movableSlots,
		int currentIndex,
		Map<Long, Place> placesById,
		RouteMode mode,
		Map<RouteEdge, RouteEstimate> estimateCache,
		RecommendedOrder bestOrder
	) {
		if (currentIndex == movableSlots.size()) {
			List<DraftSlotRequest> candidateSlots = candidateSlots(sortedSlots, movableIndexes, movableSlots);
			int totalDuration = totalRouteDuration(candidateSlots, placesById, mode, estimateCache);
			if (totalDuration < bestOrder.totalRouteDurationMinutes()) {
				return new RecommendedOrder(candidateSlots, totalDuration);
			}
			return bestOrder;
		}

		RecommendedOrder currentBestOrder = bestOrder;
		for (int index = currentIndex; index < movableSlots.size(); index++) {
			swap(movableSlots, currentIndex, index);
			currentBestOrder = searchBestOrder(
				sortedSlots,
				movableIndexes,
				movableSlots,
				currentIndex + 1,
				placesById,
				mode,
				estimateCache,
				currentBestOrder
			);
			swap(movableSlots, currentIndex, index);
		}
		return currentBestOrder;
	}

	private List<DraftSlotRequest> recommendByNearestNeighbor(
		List<DraftSlotRequest> sortedSlots,
		List<Integer> movableIndexes,
		Map<Long, Place> placesById,
		RouteMode mode,
		Map<RouteEdge, RouteEstimate> estimateCache
	) {
		List<DraftSlotRequest> recommendedSlots = new ArrayList<>(sortedSlots);
		List<DraftSlotRequest> remainingSlots = movableIndexes.stream()
			.map(sortedSlots::get)
			.collect(Collectors.toCollection(ArrayList::new));

		for (Integer movableIndex : movableIndexes) {
			DraftSlotRequest previousSlot = recommendedSlots.get(movableIndex - 1);
			DraftSlotRequest nextSlot = nearestSlot(previousSlot, remainingSlots, placesById, mode, estimateCache);
			recommendedSlots.set(movableIndex, nextSlot);
			remainingSlots.remove(nextSlot);
		}
		return recommendedSlots;
	}

	private DraftSlotRequest nearestSlot(
		DraftSlotRequest previousSlot,
		List<DraftSlotRequest> candidates,
		Map<Long, Place> placesById,
		RouteMode mode,
		Map<RouteEdge, RouteEstimate> estimateCache
	) {
		return candidates.stream()
			.min(Comparator.comparing(candidate -> estimateBetween(
				previousSlot,
				candidate,
				placesById,
				mode,
				estimateCache
			).durationMinutes()))
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.BAD_REQUEST));
	}

	private List<DraftSlotRequest> candidateSlots(
		List<DraftSlotRequest> sortedSlots,
		List<Integer> movableIndexes,
		List<DraftSlotRequest> movableSlots
	) {
		List<DraftSlotRequest> candidateSlots = new ArrayList<>(sortedSlots);
		for (int index = 0; index < movableIndexes.size(); index++) {
			candidateSlots.set(movableIndexes.get(index), movableSlots.get(index));
		}
		return candidateSlots;
	}

	private int totalRouteDuration(
		List<DraftSlotRequest> slots,
		Map<Long, Place> placesById,
		RouteMode mode,
		Map<RouteEdge, RouteEstimate> estimateCache
	) {
		int total = 0;
		for (int index = 1; index < slots.size(); index++) {
			total += estimateBetween(slots.get(index - 1), slots.get(index), placesById, mode, estimateCache)
				.durationMinutes();
		}
		return total;
	}

	private RouteEstimate estimateBetween(
		DraftSlotRequest fromSlot,
		DraftSlotRequest toSlot,
		Map<Long, Place> placesById,
		RouteMode mode,
		Map<RouteEdge, RouteEstimate> estimateCache
	) {
		RouteEdge routeEdge = new RouteEdge(fromSlot.clientId(), toSlot.clientId());
		return estimateCache.computeIfAbsent(routeEdge, key -> routeEstimator.estimate(
			mode,
			requiredPlace(fromSlot, placesById),
			requiredPlace(toSlot, placesById)
		));
	}

	private void swap(List<DraftSlotRequest> slots, int fromIndex, int toIndex) {
		DraftSlotRequest temp = slots.get(fromIndex);
		slots.set(fromIndex, slots.get(toIndex));
		slots.set(toIndex, temp);
	}

	private List<DraftSlotRequest> withSequentialOrders(List<DraftSlotRequest> slots) {
		List<DraftSlotRequest> orderedSlots = new ArrayList<>();
		for (int index = 0; index < slots.size(); index++) {
			orderedSlots.add(withOrder(slots.get(index), index + 1));
		}
		return orderedSlots;
	}

	private DraftSlotRequest withOrder(DraftSlotRequest slot, int order) {
		return new DraftSlotRequest(
			slot.clientId(),
			slot.slotId(),
			order,
			slot.title(),
			slot.placeId(),
			slot.dwellMinutes(),
			slot.locked(),
			slot.slotType(),
			slot.category()
		);
	}

	private List<DraftRouteSegmentRequest> buildRecommendedRouteSegments(
		List<DraftSlotRequest> slots,
		RouteMode recommendationMode
	) {
		List<DraftRouteSegmentRequest> routeSegments = new ArrayList<>();
		for (int index = 1; index < slots.size(); index++) {
			routeSegments.add(new DraftRouteSegmentRequest(
				slots.get(index - 1).clientId(),
				slots.get(index).clientId(),
				recommendationMode,
				0,
				false
			));
		}
		return routeSegments;
	}

	private void requireRecommendablePlaces(List<DraftSlotRequest> draftSlots, Map<Long, Place> placesById) {
		for (DraftSlotRequest draftSlot : draftSlots) {
			requiredPlace(draftSlot, placesById);
		}
	}

	private Place requiredPlace(DraftSlotRequest draftSlot, Map<Long, Place> placesById) {
		if (draftSlot.placeId() == null) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		Place place = placesById.get(draftSlot.placeId());
		if (place == null) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		return place;
	}

	public record RouteRecommendation(
		List<DraftSlotRequest> slots,
		List<DraftRouteSegmentRequest> routeSegments
	) {
	}

	private record RecommendedOrder(
		List<DraftSlotRequest> slots,
		Integer totalRouteDurationMinutes
	) {
	}

	private record RouteEdge(
		String fromClientId,
		String toClientId
	) {
	}
}
