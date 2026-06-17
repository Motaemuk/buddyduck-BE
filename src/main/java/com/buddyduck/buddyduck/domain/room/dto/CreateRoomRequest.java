package com.buddyduck.buddyduck.domain.room.dto;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateRoomRequest(
	@NotNull
	Long concertId,

	@NotBlank
	String title,

	String description,

	@NotNull
	@Positive
	Integer maxMembers,

	@NotEmpty
	List<InterestTag> roomTags,

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
