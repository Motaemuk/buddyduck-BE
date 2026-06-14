package com.buddyduck.buddyduck.domain.schedule.repository;

import com.buddyduck.buddyduck.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
}
