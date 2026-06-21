package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

public record UpdateRoomRequest(
	@NotBlank
	String title,

	String description,

	@NotNull
	@Min(2)
	Integer maxMembers,

	@NotEmpty
	List<@NotNull InterestTag> roomTags,

	@NotNull
	OffsetDateTime meetingAt,

	@Valid
	@NotNull
	RoomPlaceRequest meetingPlace,

	@Valid
	@NotNull
	RoomPlaceRequest eventPlace,

	@NotBlank
	String openChatUrl,

	String openChatPassword
) {
}
