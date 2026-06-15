package com.buddyduck.buddyduck.domain.schedule.repository;

import com.buddyduck.buddyduck.domain.schedule.entity.RouteSegment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {

	List<RouteSegment> findAllByScheduleIdOrderByIdAsc(Long scheduleId);

	void deleteAllByScheduleId(Long scheduleId);
}
