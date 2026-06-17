package com.buddyduck.buddyduck.domain.concert.repository;

import com.buddyduck.buddyduck.domain.concert.entity.ConcertInterestTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertInterestTagRepository extends JpaRepository<ConcertInterestTag, Long> {

	List<ConcertInterestTag> findAllByUserIdAndConcertIdOrderByIdAsc(Long userId, Long concertId);

	void deleteAllByUserIdAndConcertId(Long userId, Long concertId);
}
