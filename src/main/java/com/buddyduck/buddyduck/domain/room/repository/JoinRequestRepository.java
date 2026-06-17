package com.buddyduck.buddyduck.domain.room.repository;

import com.buddyduck.buddyduck.domain.room.entity.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
}
