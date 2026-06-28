package com.zigu.ziguwas.domains.notification.repository;

import com.zigu.ziguwas.domains.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {

	/**
	 * 사용자 알림을 최신순으로 페이지 조회합니다.
	 */
	Page<Notification> findAllByUserIdOrderByRecTimeDesc(Long userId, Pageable pageable);

	/**
	 * 본인 소유 알림만 단건 조회합니다.
	 */
	Optional<Notification> findByIdAndUserId(String id, Long userId);

	/**
	 * 사용자 미읽음 알림 개수를 조회합니다.
	 */
	long countByUserIdAndIsReadFalse(Long userId);
}
