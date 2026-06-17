package com.buddyduck.buddyduck.domain.concert.entity;

import com.buddyduck.buddyduck.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "concerts",
	indexes = {
		@Index(name = "idx_concerts_start_at", columnList = "start_at"),
		@Index(name = "idx_concerts_title", columnList = "title")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_concerts_source_external_id", columnNames = {"source", "external_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "external_id", nullable = false, length = 100)
	private String externalId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "venue_name", nullable = false, length = 200)
	private String venueName;

	@Column(name = "start_at", nullable = false)
	private LocalDateTime startAt;

	@Column(name = "end_at")
	private LocalDateTime endAt;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal lat;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal lng;

	@Column(nullable = false, length = 30)
	private String source;

	public static Concert create(
		String externalId,
		String title,
		String venueName,
		LocalDateTime startAt,
		LocalDateTime endAt,
		BigDecimal lat,
		BigDecimal lng,
		String source
	) {
		Concert concert = new Concert();
		concert.externalId = externalId;
		concert.title = title;
		concert.venueName = venueName;
		concert.startAt = startAt;
		concert.endAt = endAt;
		concert.lat = lat;
		concert.lng = lng;
		concert.source = source;
		return concert;
	}
}
