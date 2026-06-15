package com.buddyduck.buddyduck.domain.room.entity;

import com.buddyduck.buddyduck.domain.room.enums.RoomMemberRole;
import com.buddyduck.buddyduck.domain.user.entity.User;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "room_members",
	indexes = {
		@Index(name = "idx_room_members_user", columnList = "user_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_room_members_room_user", columnNames = {"room_id", "user_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RoomMemberRole role;

	@Column(name = "joined_at", nullable = false)
	private LocalDateTime joinedAt;

	public static RoomMember create(Room room, User user, RoomMemberRole role) {
		RoomMember member = new RoomMember();
		member.room = room;
		member.user = user;
		member.role = role;
		member.joinedAt = LocalDateTime.now();
		return member;
	}
}
