package com.buddyduck.buddyduck.domain.concert.service;

import com.buddyduck.buddyduck.domain.concert.dto.ConcertDetailResponse;
import com.buddyduck.buddyduck.domain.concert.dto.ConcertListResponse;
import com.buddyduck.buddyduck.domain.concert.dto.ConcertSummaryResponse;
import com.buddyduck.buddyduck.domain.concert.dto.InterestTagRequest;
import com.buddyduck.buddyduck.domain.concert.dto.InterestTagResponse;
import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.entity.ConcertInterestTag;
import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import com.buddyduck.buddyduck.domain.concert.kopis.KopisConcertSyncService;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertInterestTagRepository;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import com.buddyduck.buddyduck.domain.room.enums.RoomStatus;
import com.buddyduck.buddyduck.domain.room.repository.RoomRepository;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConcertService {

	private static final int MAX_PAGE_SIZE = 50;

	private final ConcertRepository concertRepository;
	private final ConcertInterestTagRepository concertInterestTagRepository;
	private final UserRepository userRepository;
	private final KopisConcertSyncService kopisConcertSyncService;
	private final RoomRepository roomRepository;

	public ConcertListResponse getConcerts(
		String keyword,
		LocalDate from,
		LocalDate to,
		String region,
		int page,
		int size
	) {
		if (page < 0 || size <= 0) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}

		kopisConcertSyncService.syncConcerts(keyword, from, to, page, size);

		Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
		Page<Concert> concertPage = concertRepository.search(
			normalize(keyword),
			startOfDay(from),
			endExclusive(to),
			normalize(region),
			pageable
		);
		List<Concert> concerts = concertPage.getContent();
		Map<Long, Long> openRoomCounts = openRoomCounts(concerts);
		List<ConcertSummaryResponse> items = concerts
			.stream()
			.map(concert -> ConcertSummaryResponse.from(concert, openRoomCounts.getOrDefault(concert.getId(), 0L)))
			.toList();

		return new ConcertListResponse(items, concertPage.getNumber(), concertPage.getSize(), concertPage.hasNext());
	}

	@Transactional(readOnly = true)
	public ConcertDetailResponse getConcert(Long concertId) {
		Concert concert = getConcertOrThrow(concertId);
		long openRoomCount = roomRepository.countByConcertIdAndStatus(concertId, RoomStatus.OPEN);
		return ConcertDetailResponse.from(concert, openRoomCount);
	}

	@Transactional(readOnly = true)
	public InterestTagResponse getMyInterestTags(Long concertId, Long userId) {
		validateConcertAndUser(concertId, userId);
		List<InterestTag> tags = concertInterestTagRepository
			.findAllByUserIdAndConcertIdOrderByIdAsc(userId, concertId)
			.stream()
			.map(ConcertInterestTag::getTag)
			.toList();
		return new InterestTagResponse(tags);
	}

	@Transactional
	public InterestTagResponse updateMyInterestTags(Long concertId, Long userId, InterestTagRequest request) {
		Concert concert = getConcertOrThrow(concertId);
		User user = getUserOrThrow(userId);
		List<InterestTag> tags = deduplicate(request.tags());

		concertInterestTagRepository.deleteAllByUserIdAndConcertId(userId, concertId);
		List<ConcertInterestTag> entities = tags.stream()
			.map(tag -> ConcertInterestTag.create(user, concert, tag))
			.toList();
		concertInterestTagRepository.saveAll(entities);

		return new InterestTagResponse(tags);
	}

	private void validateConcertAndUser(Long concertId, Long userId) {
		getConcertOrThrow(concertId);
		getUserOrThrow(userId);
	}

	private Concert getConcertOrThrow(Long concertId) {
		return concertRepository.findById(concertId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	private User getUserOrThrow(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
	}

	private List<InterestTag> deduplicate(List<InterestTag> tags) {
		if (tags == null || tags.contains(null)) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		return List.copyOf(new LinkedHashSet<>(tags));
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
	}

	private LocalDateTime startOfDay(LocalDate date) {
		return date == null ? null : date.atStartOfDay();
	}

	private LocalDateTime endExclusive(LocalDate date) {
		return date == null ? null : date.plusDays(1).atStartOfDay();
	}

	private Map<Long, Long> openRoomCounts(List<Concert> concerts) {
		if (concerts.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Long> concertIds = concerts.stream()
			.map(Concert::getId)
			.toList();

		return roomRepository.countRoomsByConcertIdsAndStatus(concertIds, RoomStatus.OPEN)
			.stream()
			.collect(Collectors.toMap(
				RoomRepository.ConcertOpenRoomCount::getConcertId,
				RoomRepository.ConcertOpenRoomCount::getOpenRoomCount,
				(left, right) -> left
			));
	}
}
