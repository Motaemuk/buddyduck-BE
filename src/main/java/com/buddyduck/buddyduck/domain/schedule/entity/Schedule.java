package com.buddyduck.buddyduck.domain.schedule.entity;

import com.buddyduck.buddyduck.domain.room.entity.Room;
import com.buddyduck.buddyduck.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false, unique = true)
	private Room room;

	@Column(name = "arrival_buffer_minutes", nullable = false)
	private Integer arrivalBufferMinutes = 30;

	@Version
	@Column(nullable = false)
	private Integer version;

	public static Schedule create(Room room) {
		Schedule schedule = new Schedule();
		schedule.room = room;
		schedule.arrivalBufferMinutes = 30;
		return schedule;
	}

	public void updateArrivalBufferMinutes(Integer arrivalBufferMinutes) {
		this.arrivalBufferMinutes = arrivalBufferMinutes;
	}
}
