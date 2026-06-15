package com.buddyduck.buddyduck.domain.room.entity;

import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "room_tags",
	indexes = {
		@Index(name = "idx_room_tags_tag", columnList = "tag")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_room_tags_room_tag", columnNames = {"room_id", "tag"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomTag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private InterestTag tag;

	public static RoomTag create(Room room, InterestTag tag) {
		RoomTag roomTag = new RoomTag();
		roomTag.room = room;
		roomTag.tag = tag;
		return roomTag;
	}
}
