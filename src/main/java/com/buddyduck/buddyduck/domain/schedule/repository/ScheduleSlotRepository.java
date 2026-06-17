package com.buddyduck.buddyduck.domain.schedule.repository;

import com.buddyduck.buddyduck.domain.schedule.entity.ScheduleSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

	List<ScheduleSlot> findAllByScheduleIdOrderBySortOrderAsc(Long scheduleId);

	void deleteAllByScheduleId(Long scheduleId);
}
