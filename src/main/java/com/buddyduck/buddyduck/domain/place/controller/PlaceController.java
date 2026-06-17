package com.buddyduck.buddyduck.domain.place.controller;

import com.buddyduck.buddyduck.domain.place.dto.GeocodeResponse;
import com.buddyduck.buddyduck.domain.place.dto.PlaceSearchResponse;
import com.buddyduck.buddyduck.domain.place.dto.PlaceUpsertRequest;
import com.buddyduck.buddyduck.domain.place.dto.PlaceUpsertResponse;
import com.buddyduck.buddyduck.domain.place.service.PlaceService;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

	private final PlaceService placeService;

	@GetMapping("/search")
	public ApiResponse<PlaceSearchResponse> searchPlaces(
		@RequestParam String keyword,
		@RequestParam(required = false) Long concertId,
		@RequestParam(required = false) Long roomId
	) {
		// Reserved for context-aware Kakao Local search; DB-backed fallback search ignores them for now.
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			placeService.searchPlaces(keyword, concertId, roomId)
		);
	}

	@GetMapping("/geocode")
	public ApiResponse<GeocodeResponse> geocode(@RequestParam String address) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, placeService.geocode(address));
	}

	@PostMapping
	public ApiResponse<PlaceUpsertResponse> upsertPlace(@Valid @RequestBody PlaceUpsertRequest request) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, placeService.upsertPlace(request));
	}
}
