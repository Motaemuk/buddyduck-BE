package com.buddyduck.buddyduck.domain.user.repository;

import com.buddyduck.buddyduck.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
