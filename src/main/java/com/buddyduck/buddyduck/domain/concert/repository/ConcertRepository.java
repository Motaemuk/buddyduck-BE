package com.buddyduck.buddyduck.domain.concert.repository;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertRepository extends JpaRepository<Concert, Long> {
}
