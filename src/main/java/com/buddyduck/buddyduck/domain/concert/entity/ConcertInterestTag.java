package com.buddyduck.buddyduck.domain.concert.entity;

import com.buddyduck.buddyduck.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "concert_interest_tags",
	indexes = {
		@Index(name = "idx_concert_interest_tags_concert_tag", columnList = "concert_id, tag")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_concert_interest_tags_user_concert_tag", columnNames = {"user_id", "concert_id", "tag"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertInterestTag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "concert_id", nullable = false)
	private Concert concert;

	@Column(nullable = false, length = 50)
	private String tag;
}
