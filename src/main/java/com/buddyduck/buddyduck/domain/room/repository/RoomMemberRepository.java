package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.RoomMember;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

	boolean existsByRoomIdAndUserId(Long roomId, Long userId);

	Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

	long countByRoomId(Long roomId);

	long countByUserId(Long userId);

	@Query("""
		select count(member.id)
		from RoomMember member
		where member.user.id = :userId
		  and member.room.status <> com.buddyduck.buddyduck.domain.room.enums.RoomStatus.CLOSED
		  and member.room.concert.startAt >= :todayStart
		""")
	long countActiveByUserId(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);

	List<RoomMember> findAllByRoomIdOrderByJoinedAtAscIdAsc(Long roomId);

	List<RoomMember> findAllByUserIdOrderByJoinedAtDesc(Long userId);

	@Query("""
		select member.room.id as roomId, count(member.id) as memberCount
		from RoomMember member
		where member.room.id in :roomIds
		group by member.room.id
		""")
	List<RoomMemberCount> countMembersByRoomIds(@Param("roomIds") Collection<Long> roomIds);

	interface RoomMemberCount {

		Long getRoomId();

		Long getMemberCount();
	}
}
