package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.JoinRequest;
import com.buddyduck.buddyduck.domain.room.enums.JoinRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

	boolean existsByRoomIdAndUserId(Long roomId, Long userId);

	Optional<JoinRequest> findByRoomIdAndUserId(Long roomId, Long userId);

	List<JoinRequest> findAllByRoomIdAndStatusOrderByCreatedAtAsc(Long roomId, JoinRequestStatus status);

	long countByRoomIdAndStatus(Long roomId, JoinRequestStatus status);

	List<JoinRequest> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
