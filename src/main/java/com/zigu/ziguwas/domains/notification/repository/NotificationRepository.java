package com.zigu.ziguwas.domains.notification.repository;

import com.zigu.ziguwas.domains.notification.entity.Notification;
import com.zigu.ziguwas.domains.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/**
	 * 사용자 알림을 최신순으로 페이지 조회합니다.
	 */
	Page<Notification> findAllByUserOrderByRecTimeDesc(User user, Pageable pageable);

	/**
	 * 본인 소유 알림만 단건 조회합니다.
	 */
	Optional<Notification> findByNotiIdAndUser(Long notiId, User user);

	/**
	 * 사용자 미읽음 알림 개수를 조회합니다.
	 */
	long countByUserAndIsReadFalse(User user);
}
