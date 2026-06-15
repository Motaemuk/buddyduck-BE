package com.buddyduck.buddyduck.domain.room.controller;

import com.buddyduck.buddyduck.domain.room.dto.DemoRoomSeedResponse;
import com.buddyduck.buddyduck.domain.room.service.DevRoomSeedService;
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
public class DevRoomSeedController {

	private final DevRoomSeedService devRoomSeedService;

	@PostMapping("/demo-room")
	public ApiResponse<DemoRoomSeedResponse> seedDemoRoom() {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, devRoomSeedService.seedDemoRoom());
	}
}
