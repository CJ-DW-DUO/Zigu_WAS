package com.zigu.ziguwas.domains.notification.scheduler;

import com.zigu.ziguwas.domains.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 90일이 지난 알림을 매일 지웁니다.
 *
 * MongoDB 시절에는 TTL 인덱스(expireAfter = P90D)가 알아서 지웠지만, PostgreSQL 에는 그런 기능이 없어 이 배치가 대신합니다.
 * Mongo TTL 은 약 1분마다 돌았고 이 배치는 하루 한 번이라, 지워지는 시점이 최대 하루 늦어질 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetentionScheduler {

    private final NotificationRepository notificationRepository;

    // Mongo TTL 때와 같이 코드에 고정한 보관기간(일)
    private static final int RETENTION_DAYS = 90;

    /**
     * 매일 새벽 4시 30분(KST)에 실행됩니다. (4시 채팅방 보관 배치와 겹치지 않게 어긋나 있음)
     */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredNotifications() {
        int deleted = notificationRepository.deleteByRecTimeBefore(LocalDateTime.now().minusDays(RETENTION_DAYS));
        log.info("보관기간({}일)이 지난 알림 {}건 삭제", RETENTION_DAYS, deleted);
    }
}
