package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.RoomMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	boolean existsByRoomIdAndUserId(Long roomId, Long userId);

	Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

	long countByRoomId(Long roomId);

	List<RoomMember> findAllByUserIdOrderByJoinedAtDesc(Long userId);
}
