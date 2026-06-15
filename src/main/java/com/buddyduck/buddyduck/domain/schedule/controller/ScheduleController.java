package com.buddyduck.buddyduck.domain.schedule.controller;

import com.buddyduck.buddyduck.domain.schedule.dto.DraftScheduleRequest;
import com.buddyduck.buddyduck.domain.schedule.dto.DraftScheduleResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.ScheduleMapResponse;
import com.buddyduck.buddyduck.domain.schedule.dto.TimelineResponse;
import com.buddyduck.buddyduck.domain.schedule.service.ScheduleService;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import com.buddyduck.buddyduck.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScheduleController {

	private final ScheduleService scheduleService;

	@GetMapping("/rooms/{roomId}/timeline")
	public ApiResponse<TimelineResponse> getTimeline(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, scheduleService.getTimeline(roomId, principal.userId()));
	}

	@GetMapping("/rooms/{roomId}/map")
	public ApiResponse<ScheduleMapResponse> getMap(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, scheduleService.getMap(roomId, principal.userId()));
	}

	@PostMapping("/schedules/{scheduleId}/draft/recalculate")
	public ApiResponse<DraftScheduleResponse> recalculateDraft(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody DraftScheduleRequest request
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			scheduleService.recalculateDraft(scheduleId, principal.userId(), request)
		);
	}

	@PutMapping("/schedules/{scheduleId}/draft/commit")
	public ApiResponse<TimelineResponse> commitDraft(
		@PathVariable Long scheduleId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody DraftScheduleRequest request
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			scheduleService.commitDraft(scheduleId, principal.userId(), request)
		);
	}
}
