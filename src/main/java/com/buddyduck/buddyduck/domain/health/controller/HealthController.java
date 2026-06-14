package com.buddyduck.buddyduck.domain.health.controller;

import com.buddyduck.buddyduck.domain.health.dto.HealthResponseDto;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	@GetMapping
	public ApiResponse<HealthResponseDto> getHealth() {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, HealthResponseDto.up());
	}
}
