package com.zigu.ziguwas.domains.notification.repository;

import com.zigu.ziguwas.domains.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findAllByUserId(Long userId);

    void deleteByTokenAndUserId(String token, Long userId);

    void deleteByToken(String token);
}
