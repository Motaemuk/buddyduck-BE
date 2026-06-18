package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.Room;
import com.buddyduck.buddyduck.domain.room.enums.RoomStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

	List<Room> findAllByConcertIdOrderByCreatedAtDesc(Long concertId);

	List<Room> findAllByHostUserIdOrderByCreatedAtDesc(Long hostUserId);

	@Query("""
		select room.concert.id as concertId, count(room.id) as openRoomCount
		from Room room
		where room.concert.id in :concertIds
		  and room.status = :status
		group by room.concert.id
		""")
	List<ConcertOpenRoomCount> countRoomsByConcertIdsAndStatus(
		@Param("concertIds") Collection<Long> concertIds,
		@Param("status") RoomStatus status
	);

	long countByConcertIdAndStatus(Long concertId, RoomStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select room from Room room where room.id = :roomId")
	Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

	interface ConcertOpenRoomCount {

		Long getConcertId();

		Long getOpenRoomCount();
	}
}
