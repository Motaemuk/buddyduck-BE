package com.buddyduck.buddyduck.domain.concert.controller;

import com.buddyduck.buddyduck.domain.concert.dto.ConcertDetailResponse;
import com.buddyduck.buddyduck.domain.concert.dto.ConcertListResponse;
import com.buddyduck.buddyduck.domain.concert.dto.InterestTagRequest;
import com.buddyduck.buddyduck.domain.concert.dto.InterestTagResponse;
import com.buddyduck.buddyduck.domain.concert.service.ConcertService;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import com.buddyduck.buddyduck.global.security.UserPrincipal;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/concerts")
@RequiredArgsConstructor
public class ConcertController {

	private final ConcertService concertService;

	@GetMapping
	public ApiResponse<ConcertListResponse> getConcerts(
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
		@RequestParam(required = false) String region,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			concertService.getConcerts(keyword, from, to, region, page, size)
		);
	}

	@GetMapping("/{concertId}")
	public ApiResponse<ConcertDetailResponse> getConcert(@PathVariable Long concertId) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, concertService.getConcert(concertId));
	}

	@GetMapping("/{concertId}/interest-tags/me")
	public ApiResponse<InterestTagResponse> getMyInterestTags(
		@PathVariable Long concertId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			concertService.getMyInterestTags(concertId, principal.userId())
		);
	}

	@PutMapping("/{concertId}/interest-tags/me")
	public ApiResponse<InterestTagResponse> updateMyInterestTags(
		@PathVariable Long concertId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody InterestTagRequest request
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			concertService.updateMyInterestTags(concertId, principal.userId(), request)
		);
	}
}
