package com.buddyduck.buddyduck.domain.schedule.repository;

import com.buddyduck.buddyduck.domain.schedule.entity.RouteSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {
}
