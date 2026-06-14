package com.buddyduck.buddyduck.domain.place.entity;

import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import com.buddyduck.buddyduck.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "places",
	indexes = {
		@Index(name = "idx_places_lat_lng", columnList = "lat, lng")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_places_provider_place_id", columnNames = {"provider", "provider_place_id"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PlaceSource provider;

	@Column(name = "provider_place_id", length = 100)
	private String providerPlaceId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 300)
	private String address;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal lat;

	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal lng;
}
