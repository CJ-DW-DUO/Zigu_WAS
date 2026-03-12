package com.zigu.ziguwas.domains.user.repository;

import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author 최정
 * @since 2026.03.12
 *
 * User JPA Repository
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일 기반 탐색
     *
     * @param email
     * @return User || null에 대한 예외처리 (orElse 사용필수)
     */
    Optional<User> findByEmail(String email);
}
