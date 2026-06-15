package com.buddyduck.buddyduck.domain.user.controller;

import com.buddyduck.buddyduck.domain.user.dto.UserProfileResponse;
import com.buddyduck.buddyduck.domain.user.service.UserQueryService;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import com.buddyduck.buddyduck.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserQueryService userQueryService;

	@GetMapping("/me")
	public ApiResponse<UserProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, userQueryService.getMyProfile(principal.userId()));
	}
}
