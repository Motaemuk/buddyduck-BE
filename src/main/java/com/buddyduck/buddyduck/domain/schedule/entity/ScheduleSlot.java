package com.buddyduck.buddyduck.domain.schedule.entity;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotCategory;
import com.buddyduck.buddyduck.domain.schedule.enums.SlotType;
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
	name = "schedule_slots",
	indexes = {
		@Index(name = "idx_schedule_slots_place", columnList = "place_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_schedule_slots_schedule_sort_order", columnNames = {"schedule_id", "sort_order"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSlot extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id", nullable = false)
	private Schedule schedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id")
	private Place place;

	@Enumerated(EnumType.STRING)
	@Column(name = "slot_type", nullable = false, length = 20)
	private SlotType slotType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SlotCategory category;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Column(name = "end_at", nullable = false)
	private LocalDateTime endAt;

	@Column(name = "dwell_minutes", nullable = false)
	private Integer dwellMinutes;

	@Column(nullable = false)
	private Boolean locked = false;
}
