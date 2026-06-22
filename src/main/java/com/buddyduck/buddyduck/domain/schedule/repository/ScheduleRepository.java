package com.buddyduck.buddyduck.domain.schedule.repository;

import com.buddyduck.buddyduck.domain.schedule.entity.Schedule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

	Optional<Schedule> findByRoomId(Long roomId);

	@Query("""
		select schedule
		from Schedule schedule
		join fetch schedule.room room
		join fetch room.concert
		where schedule.id = :scheduleId
		""")
	Optional<Schedule> findByIdWithRoomAndConcert(@Param("scheduleId") Long scheduleId);
}
