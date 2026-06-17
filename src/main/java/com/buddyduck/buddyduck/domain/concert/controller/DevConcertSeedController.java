package com.buddyduck.buddyduck.domain.concert.controller;

import com.buddyduck.buddyduck.domain.concert.dto.SeedConcertsResponse;
import com.buddyduck.buddyduck.domain.concert.service.ConcertSeedService;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "test"})
@RestController
@RequestMapping("/api/dev/seed")
@RequiredArgsConstructor
public class DevConcertSeedController {

	private final ConcertSeedService concertSeedService;

	@PostMapping("/concerts")
	public ApiResponse<SeedConcertsResponse> seedConcerts() {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, concertSeedService.seedConcerts());
	}
}
