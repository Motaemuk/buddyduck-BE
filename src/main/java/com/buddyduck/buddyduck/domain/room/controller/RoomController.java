package com.buddyduck.buddyduck.domain.room.controller;

import com.buddyduck.buddyduck.domain.room.dto.CreateRoomRequest;
import com.buddyduck.buddyduck.domain.room.dto.CreateRoomResponse;
import com.buddyduck.buddyduck.domain.room.dto.MyRoomListResponse;
import com.buddyduck.buddyduck.domain.room.dto.OpenChatResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomDetailResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomLeaveResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomListResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomManagementResponse;
import com.buddyduck.buddyduck.domain.room.dto.UpdateRoomRequest;
import com.buddyduck.buddyduck.domain.room.service.RoomService;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralSuccessCode;
import com.buddyduck.buddyduck.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

	private final RoomService roomService;

	@GetMapping("/concerts/{concertId}/rooms")
	public ApiResponse<RoomListResponse> getRoomsByConcert(
		@PathVariable Long concertId,
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(required = false) String status,
		@RequestParam(required = false) String tags,
		@RequestParam(required = false) Integer minMatchCount,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size
	) {
		return ApiResponse.onSuccess(
			GeneralSuccessCode.OK,
			roomService.getRoomsByConcert(concertId, principal.userId(), status, tags, minMatchCount, page, size)
		);
	}

	@PostMapping("/rooms")
	public ApiResponse<CreateRoomResponse> createRoom(
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody CreateRoomRequest request
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.createRoom(principal.userId(), request));
	}

	@GetMapping("/rooms/{roomId}")
	public ApiResponse<RoomDetailResponse> getRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.getRoom(roomId, principal.userId()));
	}

	@GetMapping("/me/rooms")
	public ApiResponse<MyRoomListResponse> getMyRooms(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(required = false) String tab
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.getMyRooms(principal.userId(), tab));
	}

	@GetMapping("/rooms/{roomId}/open-chat")
	public ApiResponse<OpenChatResponse> getOpenChat(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.getOpenChat(roomId, principal.userId()));
	}

	@DeleteMapping("/rooms/{roomId}/members/me")
	public ApiResponse<RoomLeaveResponse> leaveRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.leaveRoom(roomId, principal.userId()));
	}

	@PatchMapping("/rooms/{roomId}")
	public ApiResponse<RoomManagementResponse> updateRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal,
		@Valid @RequestBody UpdateRoomRequest request
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.updateRoom(roomId, principal.userId(), request));
	}

	@PatchMapping("/rooms/{roomId}/close")
	public ApiResponse<RoomManagementResponse> closeRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.closeRoom(roomId, principal.userId()));
	}

	@DeleteMapping("/rooms/{roomId}")
	public ApiResponse<RoomManagementResponse> deleteRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		return ApiResponse.onSuccess(GeneralSuccessCode.OK, roomService.deleteRoom(roomId, principal.userId()));
	}
}
