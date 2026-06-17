package com.buddyduck.buddyduck.domain.user.repository;

import com.buddyduck.buddyduck.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKakaoId(String kakaoId);
}
