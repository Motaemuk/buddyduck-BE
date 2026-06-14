package com.buddyduck.buddyduck.domain.room.entity;

import com.buddyduck.buddyduck.domain.room.enums.JoinRequestStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "join_requests",
	indexes = {
		@Index(name = "idx_join_requests_room_status", columnList = "room_id, status"),
		@Index(name = "idx_join_requests_user_status", columnList = "user_id, status")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_join_requests_room_user", columnNames = {"room_id", "user_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JoinRequest extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(length = 300)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private JoinRequestStatus status = JoinRequestStatus.PENDING;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "decided_by")
	private User decidedBy;

	@Column(name = "decided_at")
	private LocalDateTime decidedAt;
}
