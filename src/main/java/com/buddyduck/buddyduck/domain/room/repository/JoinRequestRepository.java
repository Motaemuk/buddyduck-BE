package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.JoinRequest;
import com.buddyduck.buddyduck.domain.room.enums.JoinRequestStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

	boolean existsByRoomIdAndUserId(Long roomId, Long userId);

	Optional<JoinRequest> findByRoomIdAndUserId(Long roomId, Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select request
		from JoinRequest request
		where request.room.id = :roomId
		  and request.user.id = :userId
		""")
	Optional<JoinRequest> findByRoomIdAndUserIdForUpdate(@Param("roomId") Long roomId, @Param("userId") Long userId);

	List<JoinRequest> findAllByRoomIdAndStatusOrderByCreatedAtAsc(Long roomId, JoinRequestStatus status);

	long countByRoomIdAndStatus(Long roomId, JoinRequestStatus status);

	long countByUserIdAndStatus(Long userId, JoinRequestStatus status);

	@Query("""
		select count(request.id)
		from JoinRequest request
		where request.user.id = :userId
		  and request.status = :status
		  and (
		    (request.room.concert.endAt is not null and request.room.concert.endAt >= :now)
		    or (request.room.concert.endAt is null and request.room.concert.startAt >= :now)
		  )
		""")
	long countActiveByUserIdAndStatus(
		@Param("userId") Long userId,
		@Param("status") JoinRequestStatus status,
		@Param("now") LocalDateTime now
	);

	List<JoinRequest> findAllByUserIdOrderByCreatedAtDesc(Long userId);

	@Query("""
		select request.room.id as roomId, count(request.id) as pendingRequestCount
		from JoinRequest request
		where request.room.id in :roomIds
		  and request.status = :status
		group by request.room.id
		""")
	List<RoomPendingRequestCount> countRequestsByRoomIdsAndStatus(
		@Param("roomIds") Collection<Long> roomIds,
		@Param("status") JoinRequestStatus status
	);

	interface RoomPendingRequestCount {

		Long getRoomId();

		Long getPendingRequestCount();
	}
}
