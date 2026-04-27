package com.zigu.ziguwas.domains.notification.repository;

import com.zigu.ziguwas.domains.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
