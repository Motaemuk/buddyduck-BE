package com.buddyduck.buddyduck.domain.room.entity;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.room.enums.RoomStatus;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "rooms",
	indexes = {
		@Index(name = "idx_rooms_concert_status", columnList = "concert_id, status"),
		@Index(name = "idx_rooms_host_user", columnList = "host_user_id")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "concert_id", nullable = false)
	private Concert concert;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "host_user_id", nullable = false)
	private User hostUser;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(length = 500)
	private String description;

	@Column(name = "max_members", nullable = false)
	private Integer maxMembers;

	@Column(name = "meeting_at", nullable = false)
	private LocalDateTime meetingAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_place_id", nullable = false)
	private Place meetingPlace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "event_place_id", nullable = false)
	private Place eventPlace;

	@Column(name = "open_chat_url", nullable = false, length = 500)
	private String openChatUrl;

	@Column(name = "open_chat_password", length = 100)
	private String openChatPassword;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoomStatus status = RoomStatus.OPEN;

	public static Room create(
		Concert concert,
		User hostUser,
		String title,
		String description,
		Integer maxMembers,
		LocalDateTime meetingAt,
		Place meetingPlace,
		Place eventPlace,
		String openChatUrl,
		String openChatPassword
	) {
		Room room = new Room();
		room.concert = concert;
		room.hostUser = hostUser;
		room.title = title;
		room.description = description;
		room.maxMembers = maxMembers;
		room.meetingAt = meetingAt;
		room.meetingPlace = meetingPlace;
		room.eventPlace = eventPlace;
		room.openChatUrl = openChatUrl;
		room.openChatPassword = openChatPassword;
		room.status = RoomStatus.OPEN;
		return room;
	}

	public void markFull() {
		this.status = RoomStatus.FULL;
	}
}
