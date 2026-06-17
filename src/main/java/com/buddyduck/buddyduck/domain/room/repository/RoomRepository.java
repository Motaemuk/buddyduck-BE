package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.Room;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

	List<Room> findAllByConcertIdOrderByCreatedAtDesc(Long concertId);

	List<Room> findAllByHostUserIdOrderByCreatedAtDesc(Long hostUserId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select room from Room room where room.id = :roomId")
	Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);
}
