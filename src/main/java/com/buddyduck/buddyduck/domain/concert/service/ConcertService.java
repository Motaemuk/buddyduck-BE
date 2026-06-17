package com.buddyduck.buddyduck.domain.concert.service;

import com.buddyduck.buddyduck.domain.concert.dto.ConcertDetailResponse;
import com.buddyduck.buddyduck.domain.concert.dto.ConcertListResponse;
import com.buddyduck.buddyduck.domain.concert.dto.ConcertSummaryResponse;
import com.buddyduck.buddyduck.domain.concert.dto.InterestTagRequest;
import com.buddyduck.buddyduck.domain.concert.dto.InterestTagResponse;
import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.entity.ConcertInterestTag;
import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertInterestTagRepository;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
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

	@Transactional(readOnly = true)
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

		Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
		Page<Concert> concertPage = concertRepository.search(
			normalize(keyword),
			startOfDay(from),
			endExclusive(to),
			normalize(region),
			pageable
		);
		List<ConcertSummaryResponse> items = concertPage.getContent()
			.stream()
			.map(ConcertSummaryResponse::from)
			.toList();

		return new ConcertListResponse(items, concertPage.getNumber(), concertPage.getSize(), concertPage.hasNext());
	}

	@Transactional(readOnly = true)
	public ConcertDetailResponse getConcert(Long concertId) {
		Concert concert = getConcertOrThrow(concertId);
		return ConcertDetailResponse.from(concert);
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
}
