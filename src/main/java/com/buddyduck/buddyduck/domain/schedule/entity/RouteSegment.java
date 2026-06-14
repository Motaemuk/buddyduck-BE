package com.buddyduck.buddyduck.domain.schedule.entity;

import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
	name = "route_segments",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_route_segments_schedule_from_to",
			columnNames = {"schedule_id", "from_slot_id", "to_slot_id"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteSegment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "schedule_id", nullable = false)
	private Schedule schedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_slot_id", nullable = false)
	private ScheduleSlot fromSlot;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_slot_id", nullable = false)
	private ScheduleSlot toSlot;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RouteMode mode;

	@Column(name = "distance_meters")
	private Integer distanceMeters;

	@Column(name = "duration_minutes", nullable = false)
	private Integer durationMinutes;

	@Column(length = 30)
	private String provider;

	@Column(name = "manually_adjusted", nullable = false)
	private Boolean manuallyAdjusted = false;
}
