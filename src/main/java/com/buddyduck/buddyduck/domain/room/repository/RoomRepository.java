package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.Room;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

	List<Room> findAllByConcertIdOrderByCreatedAtDesc(Long concertId);

	List<Room> findAllByHostUserIdOrderByCreatedAtDesc(Long hostUserId);
}
