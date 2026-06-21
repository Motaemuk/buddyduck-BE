package com.buddyduck.buddyduck.domain.room.controller;

import com.buddyduck.buddyduck.domain.room.dto.JoinRequestApproveResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestCancelResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestCreateRequest;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestCreateResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestDecisionResponse;
import com.buddyduck.buddyduck.domain.room.dto.JoinRequestListResponse;
import com.buddyduck.buddyduck.domain.room.dto.MyJoinRequestStatusResponse;
import com.buddyduck.buddyduck.domain.room.enums.JoinRequestStatus;
import com.buddyduck.buddyduck.domain.room.service.JoinRequestService;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import com.buddyduck.buddyduck.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/join-requests")
@RequiredArgsConstructor
public class JoinRequestController {

	private final JoinRequestService joinRequestService;

	@PostMapping
	public ApiResponse<JoinRequestCreateResponse> createJoinRequest(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody JoinRequestCreateRequest request
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			joinRequestService.createJoinRequest(roomId, principal.userId(), request)
		);
	}

	@GetMapping("/me")
	public ApiResponse<MyJoinRequestStatusResponse> getMyJoinRequest(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			joinRequestService.getMyJoinRequest(roomId, principal.userId())
		);
	}

	@DeleteMapping("/me")
	public ApiResponse<JoinRequestCancelResponse> cancelMyJoinRequest(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			joinRequestService.cancelMyJoinRequest(roomId, principal.userId())
		);
	}

	@GetMapping
	public ApiResponse<JoinRequestListResponse> getJoinRequests(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(required = false) JoinRequestStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			joinRequestService.getJoinRequests(roomId, principal.userId(), status, page, size)
		);
	}

	@PostMapping("/{requestId}/approve")
	public ApiResponse<JoinRequestApproveResponse> approveJoinRequest(
		@PathVariable Long roomId,
		@PathVariable Long requestId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			joinRequestService.approveJoinRequest(roomId, requestId, principal.userId())
		);
	}

	@PostMapping("/{requestId}/reject")
	public ApiResponse<JoinRequestDecisionResponse> rejectJoinRequest(
		@PathVariable Long roomId,
		@PathVariable Long requestId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			joinRequestService.rejectJoinRequest(roomId, requestId, principal.userId())
		);
	}
}
