package com.buddyduck.buddyduck.domain.concert.repository;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

	boolean existsBySourceAndExternalId(String source, String externalId);

	Optional<Concert> findBySourceAndExternalId(String source, String externalId);

	@Query("""
		SELECT c
		FROM Concert c
		WHERE (:keyword IS NULL
			OR LOWER(c.title) LIKE CONCAT('%', :keyword, '%')
			OR LOWER(c.venueName) LIKE CONCAT('%', :keyword, '%'))
			AND (:from IS NULL OR c.startAt >= :from)
			AND (:to IS NULL OR c.startAt < :to)
			AND (:region IS NULL
				OR LOWER(c.area) LIKE CONCAT('%', :region, '%')
				OR LOWER(c.venueName) LIKE CONCAT('%', :region, '%'))
			AND (
				(c.endAt IS NOT NULL AND c.endAt >= :activeAt)
				OR (c.endAt IS NULL AND c.startAt >= :activeAt)
			)
		ORDER BY c.startAt ASC, c.id ASC
		""")
	Page<Concert> search(
		String keyword,
		LocalDateTime from,
		LocalDateTime to,
		String region,
		LocalDateTime activeAt,
		Pageable pageable
	);
}
